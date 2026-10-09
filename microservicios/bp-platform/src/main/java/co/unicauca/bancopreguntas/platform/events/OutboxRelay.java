package co.unicauca.bancopreguntas.platform.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Lee de la tabla {@code outbox_event} los eventos que aún no salieron y los publica en el broker, en el orden en
 * que se guardaron. Si el broker no responde, los deja pendientes y reintenta en el siguiente ciclo, de modo que
 * ningún evento se pierde (se entrega al menos una vez: los consumidores son idempotentes).
 *
 * <p>Con varias réplicas del servicio, cada lote se toma con {@code FOR UPDATE SKIP LOCKED}: dos réplicas nunca
 * publican el mismo evento a la vez.
 */
public class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

    static final String PENDIENTES = "SELECT seq, routing_key, payload FROM outbox_event WHERE sent_at IS NULL "
            + "ORDER BY seq LIMIT ? FOR UPDATE SKIP LOCKED";
    static final String MARCAR_ENVIADO = "UPDATE outbox_event SET sent_at = ? WHERE seq = ?";
    static final String LIMPIAR = "DELETE FROM outbox_event WHERE sent_at IS NOT NULL AND sent_at < ?";

    /** Cada cuánto, como máximo, se repite en el registro el aviso de que el broker no responde. */
    private static final Duration ENTRE_AVISOS = Duration.ofSeconds(30);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;
    private final RabbitTemplate rabbit;
    private final Clock reloj;
    private final int lote;
    private final Duration conservarEnviados;

    private Instant ultimoAviso = Instant.MIN;

    public OutboxRelay(JdbcTemplate jdbc, PlatformTransactionManager transacciones, RabbitTemplate rabbit, Clock reloj,
                       int lote, Duration conservarEnviados) {
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(transacciones);
        this.rabbit = rabbit;
        this.reloj = reloj;
        this.lote = lote;
        this.conservarEnviados = conservarEnviados;
    }

    private record Pendiente(long seq, String clave, String cuerpo) {
    }

    /**
     * Publica un lote de eventos pendientes.
     *
     * @return cuántos se publicaron; menos que los pendientes si el broker falló a mitad del lote
     */
    @Scheduled(fixedDelayString = "${bancopreguntas.events.outbox.interval:PT0.5S}")
    public int publicarPendientes() {
        Integer publicados = transaccion.execute(estado -> {
            List<Pendiente> pendientes = jdbc.query(PENDIENTES,
                    (rs, i) -> new Pendiente(rs.getLong("seq"), rs.getString("routing_key"), rs.getString("payload")), lote);
            int enviados = 0;
            for (Pendiente p : pendientes) {
                try {
                    rabbit.send(EventTopology.EXCHANGE, p.clave(), EventEnvelope.mensaje(p.cuerpo().getBytes(StandardCharsets.UTF_8)));
                } catch (RuntimeException e) {
                    avisarFalla(p, pendientes.size() - enviados, e);
                    break; // se conserva el orden: lo que sigue espera a que este salga
                }
                jdbc.update(MARCAR_ENVIADO, Timestamp.from(reloj.instant()), p.seq());
                enviados++;
            }
            if (enviados > 0) {
                LOG.debug("Bandeja de salida: {} eventos publicados", enviados);
            }
            return enviados;
        });
        return publicados == null ? 0 : publicados;
    }

    /** Borra los eventos ya enviados con más antigüedad que el período de conservación. */
    @Scheduled(fixedDelayString = "${bancopreguntas.events.outbox.cleanup-interval:PT10M}")
    public int limpiarEnviados() {
        return jdbc.update(LIMPIAR, Timestamp.from(reloj.instant().minus(conservarEnviados)));
    }

    /** Cuántos eventos esperan salir. */
    public long pendientes() {
        Long total = jdbc.queryForObject("SELECT count(*) FROM outbox_event WHERE sent_at IS NULL", Long.class);
        return total == null ? 0 : total;
    }

    private void avisarFalla(Pendiente p, long enEspera, RuntimeException e) {
        Instant ahora = reloj.instant();
        if (Duration.between(ultimoAviso, ahora).compareTo(ENTRE_AVISOS) >= 0) {
            ultimoAviso = ahora;
            LOG.warn("Bandeja de salida: no se pudo publicar el evento {} (hay {} esperando; se reintentará): {}",
                    p.clave(), enEspera, e.getMessage());
        }
    }
}
