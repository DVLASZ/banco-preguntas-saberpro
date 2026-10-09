package co.unicauca.bancopreguntas.platform.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.UUID;

/**
 * Publica eventos en el exchange común con el sobre {@code {eventId, type, occurredAt, data}}. Es la
 * mitad "publicador" del patrón publicador/suscriptor: quien publica no conoce a los suscriptores.
 *
 * <p>Si hay una transacción en curso, el mensaje se envía solo después de confirmarla, para no avisar de
 * cambios que podrían deshacerse. Un fallo del broker se registra pero no se propaga: el cambio de negocio
 * ya quedó guardado.
 */
public class AmqpEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpEventPublisher.class);

    private final RabbitTemplate rabbit;
    private final ObjectMapper json;
    private final Clock reloj;

    public AmqpEventPublisher(RabbitTemplate rabbit, ObjectMapper json, Clock reloj) {
        this.rabbit = rabbit;
        this.json = json;
        this.reloj = reloj;
    }

    @Override
    public String publicar(String tipo, Object datos) {
        String eventId = UUID.randomUUID().toString();
        Message mensaje = construir(eventId, tipo, datos);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(tipo, mensaje);
                }
            });
        } else {
            enviar(tipo, mensaje);
        }
        return eventId;
    }

    private Message construir(String eventId, String tipo, Object datos) {
        try {
            return EventEnvelope.mensaje(json.writeValueAsBytes(EventEnvelope.sobre(eventId, tipo, reloj.instant(), datos)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el evento " + tipo, e);
        }
    }

    private void enviar(String tipo, Message mensaje) {
        try {
            rabbit.send(EventTopology.EXCHANGE, tipo, mensaje);
            LOG.info("Evento publicado: {}", tipo);
        } catch (RuntimeException e) {
            LOG.error("No se pudo publicar el evento {}", tipo, e);
        }
    }
}
