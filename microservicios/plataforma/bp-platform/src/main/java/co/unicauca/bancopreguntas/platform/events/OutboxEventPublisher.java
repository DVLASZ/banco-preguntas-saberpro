package co.unicauca.bancopreguntas.platform.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

/**
 * Patrón Outbox: en lugar de enviar el evento al broker, lo guarda en la tabla {@code outbox_event} <b>en la misma
 * transacción</b> que el cambio de negocio. Si la transacción se confirma, el evento queda guardado y el
 * {@link OutboxRelay} lo publicará aunque RabbitMQ esté caído en ese momento; si se deshace, el evento tampoco
 * existe. Así nunca hay un cambio sin su aviso ni un aviso de un cambio que no ocurrió.
 */
public class OutboxEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxEventPublisher.class);

    static final String INSERTAR = "INSERT INTO outbox_event (event_id, routing_key, payload, created_at) VALUES (?, ?, ?, ?)";

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Clock reloj;

    public OutboxEventPublisher(JdbcTemplate jdbc, ObjectMapper json, Clock reloj) {
        this.jdbc = jdbc;
        this.json = json;
        this.reloj = reloj;
    }

    @Override
    public String publicar(String tipo, Object datos) {
        String eventId = UUID.randomUUID().toString();
        String cuerpo;
        try {
            cuerpo = json.writeValueAsString(EventEnvelope.sobre(eventId, tipo, reloj.instant(), datos));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el evento " + tipo, e);
        }
        jdbc.update(INSERTAR, eventId, tipo, cuerpo, Timestamp.from(reloj.instant()));
        LOG.debug("Evento {} guardado en la bandeja de salida", tipo);
        return eventId;
    }
}
