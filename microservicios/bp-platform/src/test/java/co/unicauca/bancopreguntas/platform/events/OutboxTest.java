package co.unicauca.bancopreguntas.platform.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** El Outbox sobre un PostgreSQL real: se guarda con el cambio, sale en orden y sobrevive a un broker caído. */
class OutboxTest {

    private static EmbeddedPostgres postgres;
    private static JdbcTemplate jdbc;
    private static DataSourceTransactionManager transacciones;

    private final ObjectMapper json = new ObjectMapper();
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"), ZoneOffset.UTC);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);

    private OutboxEventPublisher publicador;
    private OutboxRelay relay;

    @BeforeAll
    static void iniciarBase() throws IOException {
        postgres = EmbeddedPostgres.start();
        var datos = postgres.getPostgresDatabase();
        jdbc = new JdbcTemplate(datos);
        transacciones = new DataSourceTransactionManager(datos);
        jdbc.execute("""
                CREATE TABLE outbox_event (
                    seq BIGSERIAL PRIMARY KEY,
                    event_id VARCHAR(36) NOT NULL UNIQUE,
                    routing_key VARCHAR(100) NOT NULL,
                    payload TEXT NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL,
                    sent_at TIMESTAMPTZ)""");
    }

    @AfterAll
    static void cerrarBase() throws IOException {
        postgres.close();
    }

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE outbox_event");
        reset(rabbit);
        publicador = new OutboxEventPublisher(jdbc, json, reloj);
        relay = new OutboxRelay(jdbc, transacciones, rabbit, reloj, 100, Duration.ofHours(24));
    }

    private void enTransaccion(Runnable accion) {
        new TransactionTemplate(transacciones).executeWithoutResult(s -> accion.run());
    }

    @Test
    void publicarSoloGuardaElEventoYNoTocaElBroker() {
        String id = publicador.publicar("question.submitted", java.util.Map.of("questionId", "q1"));

        assertThat(relay.pendientes()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT event_id FROM outbox_event", String.class)).isEqualTo(id);
        verifyNoInteractions(rabbit);
    }

    @Test
    void siLaTransaccionSeDeshaceElEventoNoExiste() {
        assertThatThrownBy(() -> enTransaccion(() -> {
            publicador.publicar("question.submitted", java.util.Map.of("questionId", "q1"));
            throw new IllegalStateException("falla de negocio");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(relay.pendientes()).isZero();
    }

    @Test
    void siLaTransaccionSeConfirmaElEventoQuedaPendiente() {
        enTransaccion(() -> publicador.publicar("question.approved", java.util.Map.of("questionId", "q2")));

        assertThat(relay.pendientes()).isEqualTo(1);
    }

    @Test
    void elRelevoPublicaEnOrdenConElSobreCompletoYMarcaComoEnviado() throws Exception {
        publicador.publicar("question.submitted", java.util.Map.of("n", 1));
        publicador.publicar("question.approved", java.util.Map.of("n", 2));
        publicador.publicar("question.published", java.util.Map.of("n", 3));

        assertThat(relay.publicarPendientes()).isEqualTo(3);

        ArgumentCaptor<String> claves = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Message> mensajes = ArgumentCaptor.forClass(Message.class);
        verify(rabbit, times(3)).send(eq(EventTopology.EXCHANGE), claves.capture(), mensajes.capture());
        assertThat(claves.getAllValues()).containsExactly("question.submitted", "question.approved", "question.published");

        JsonNode sobre = json.readTree(mensajes.getAllValues().get(1).getBody());
        assertThat(sobre.get("type").asText()).isEqualTo("question.approved");
        assertThat(sobre.get("occurredAt").asText()).isEqualTo("2026-10-08T10:00:00Z");
        assertThat(sobre.get("data").get("n").asInt()).isEqualTo(2);
        assertThat(relay.pendientes()).isZero();
    }

    @Test
    void unEventoYaEnviadoNoSeVuelveAPublicar() {
        publicador.publicar("question.submitted", java.util.Map.of("n", 1));
        relay.publicarPendientes();
        reset(rabbit);

        assertThat(relay.publicarPendientes()).isZero();
        verifyNoInteractions(rabbit);
    }

    @Test
    void conElBrokerCaidoLosEventosSeConservanYSalenCuandoVuelve() {
        publicador.publicar("question.submitted", java.util.Map.of("n", 1));
        publicador.publicar("question.approved", java.util.Map.of("n", 2));
        doThrow(new AmqpConnectException(new ConnectException("caído")))
                .when(rabbit).send(any(String.class), any(String.class), any(Message.class));

        assertThat(relay.publicarPendientes()).isZero();
        assertThat(relay.publicarPendientes()).isZero();
        assertThat(relay.pendientes()).isEqualTo(2);

        reset(rabbit);
        assertThat(relay.publicarPendientes()).isEqualTo(2);
        assertThat(relay.pendientes()).isZero();
    }

    @Test
    void siFallaAMitadDelLoteSeConservaElOrden() {
        publicador.publicar("a.uno", java.util.Map.of());
        publicador.publicar("b.dos", java.util.Map.of());
        publicador.publicar("c.tres", java.util.Map.of());
        doThrow(new AmqpConnectException(new ConnectException("caído")))
                .when(rabbit).send(eq(EventTopology.EXCHANGE), eq("b.dos"), any(Message.class));

        assertThat(relay.publicarPendientes()).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT routing_key FROM outbox_event WHERE sent_at IS NULL ORDER BY seq", String.class))
                .containsExactly("b.dos", "c.tres");

        reset(rabbit);
        assertThat(relay.publicarPendientes()).isEqualTo(2);
    }

    @Test
    void doSReplicasNuncaPublicanElMismoEvento() throws Exception {
        for (int i = 0; i < 40; i++) {
            publicador.publicar("question.submitted", java.util.Map.of("n", i));
        }
        List<String> vistos = java.util.Collections.synchronizedList(new ArrayList<>());
        doAnswer(inv -> {
            vistos.add(json.readTree(inv.<Message>getArgument(2).getBody()).get("eventId").asText());
            Thread.sleep(2);
            return null;
        }).when(rabbit).send(any(String.class), any(String.class), any(Message.class));

        OutboxRelay otra = new OutboxRelay(jdbc, transacciones, rabbit, reloj, 10, Duration.ofHours(24));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> trabajos = new ArrayList<>();
        for (OutboxRelay r : List.of(relay, otra)) {
            trabajos.add(pool.submit(() -> {
                salida.await();
                int total = 0;
                int ronda;
                do {
                    ronda = r.publicarPendientes();
                    total += ronda;
                } while (ronda > 0);
                return total;
            }));
        }
        salida.countDown();
        int total = 0;
        for (Future<Integer> t : trabajos) {
            total += t.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(total).isEqualTo(40);
        assertThat(vistos).hasSize(40).doesNotHaveDuplicates();
        assertThat(relay.pendientes()).isZero();
    }

    @Test
    void laLimpiezaBorraSoloLosEnviadosAntiguos() {
        publicador.publicar("question.submitted", java.util.Map.of("n", 1));
        publicador.publicar("question.approved", java.util.Map.of("n", 2));
        relay.publicarPendientes();
        publicador.publicar("question.published", java.util.Map.of("n", 3));
        // Los dos primeros se enviaron "hace dos días"; el tercero sigue pendiente.
        jdbc.update("UPDATE outbox_event SET sent_at = ? WHERE sent_at IS NOT NULL",
                java.sql.Timestamp.from(reloj.instant().minus(Duration.ofDays(2))));

        assertThat(relay.limpiarEnviados()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event", Integer.class)).isEqualTo(1);
        assertThat(relay.pendientes()).isEqualTo(1);
    }

    @Test
    void laLimpiezaConservaLosEnviadosRecientes() {
        publicador.publicar("question.submitted", java.util.Map.of("n", 1));
        relay.publicarPendientes();

        assertThat(relay.limpiarEnviados()).isZero();
    }
}
