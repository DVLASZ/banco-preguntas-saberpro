package co.unicauca.bancopreguntas.platform.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("AmqpEventPublisher")
class AmqpEventPublisherTest {

    private static final Instant AHORA = Instant.parse("2026-10-08T15:00:00Z");

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ObjectMapper json = new ObjectMapper();
    private final AmqpEventPublisher publicador = new AmqpEventPublisher(rabbit, json, Clock.fixed(AHORA, ZoneOffset.UTC));

    record Datos(String questionId, String nota) {
    }

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Message enviado(String routingKey) {
        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("bancopreguntas.events"), eq(routingKey), mensaje.capture());
        return mensaje.getValue();
    }

    @Test
    @DisplayName("publica en el exchange común con la routing key igual al tipo")
    void usaElTipoComoRoutingKey() {
        publicador.publicar("question.submitted", new Datos("Q-1", "x"));

        assertThat(enviado("question.submitted")).isNotNull();
    }

    @Test
    @DisplayName("el cuerpo lleva el sobre {eventId, type, occurredAt, data} y devuelve el eventId")
    void cuerpoConElSobreComun() throws Exception {
        String eventId = publicador.publicar("question.submitted", new Datos("Q-1", "x"));

        JsonNode sobre = json.readTree(enviado("question.submitted").getBody());

        assertThat(sobre.get("eventId").asText()).isEqualTo(eventId);
        assertThat(UUID.fromString(eventId)).isNotNull();
        assertThat(sobre.get("type").asText()).isEqualTo("question.submitted");
        assertThat(sobre.get("occurredAt").asText()).isEqualTo("2026-10-08T15:00:00Z");
        assertThat(sobre.get("data").get("questionId").asText()).isEqualTo("Q-1");
    }

    @Test
    @DisplayName("cada evento recibe un eventId distinto")
    void eventIdsDistintos() {
        String a = publicador.publicar("a.b", Map.of("k", "v"));
        String b = publicador.publicar("a.b", Map.of("k", "v"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("el mensaje es JSON, UTF-8 y persistente")
    void propiedadesDelMensaje() {
        publicador.publicar("question.submitted", new Datos("Q-1", "x"));

        MessageProperties propiedades = enviado("question.submitted").getMessageProperties();

        assertThat(propiedades.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(propiedades.getContentEncoding()).isEqualTo("UTF-8");
        assertThat(propiedades.getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
    }

    @Test
    @DisplayName("conserva las tildes y los caracteres especiales")
    void conservaLasTildes() throws Exception {
        publicador.publicar("question.submitted", new Datos("Q-1", "Educación ñandú"));

        JsonNode datos = json.readTree(enviado("question.submitted").getBody()).get("data");

        assertThat(datos.get("nota").asText()).isEqualTo("Educación ñandú");
    }

    @Test
    @DisplayName("dentro de una transacción solo envía después de confirmarla")
    void esperaAlCommit() {
        TransactionSynchronizationManager.initSynchronization();

        publicador.publicar("question.submitted", new Datos("Q-1", "x"));

        verify(rabbit, never()).send(any(String.class), any(String.class), any(Message.class));
        List<TransactionSynchronization> sincronizaciones = TransactionSynchronizationManager.getSynchronizations();
        assertThat(sincronizaciones).hasSize(1);

        sincronizaciones.forEach(TransactionSynchronization::afterCommit);

        assertThat(enviado("question.submitted")).isNotNull();
    }

    @Test
    @DisplayName("si la transacción se deshace, no se envía nada")
    void noEnviaSiSeDeshace() {
        TransactionSynchronizationManager.initSynchronization();

        publicador.publicar("question.submitted", new Datos("Q-1", "x"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(rabbit, never()).send(any(String.class), any(String.class), any(Message.class));
    }

    @Test
    @DisplayName("si el broker falla, el error no se propaga a quien guardó el cambio")
    void unFalloDelBrokerNoSePropaga() {
        doThrow(new AmqpConnectException(new RuntimeException("sin broker")))
                .when(rabbit).send(any(String.class), any(String.class), any(Message.class));

        assertThatCode(() -> publicador.publicar("question.submitted", new Datos("Q-1", "x")))
                .doesNotThrowAnyException();
    }
}
