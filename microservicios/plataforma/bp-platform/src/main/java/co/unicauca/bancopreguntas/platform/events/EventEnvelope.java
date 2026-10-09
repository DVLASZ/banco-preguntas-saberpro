package co.unicauca.bancopreguntas.platform.events;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** El sobre común de todos los eventos: {@code {eventId, type, occurredAt, data}}, y el mensaje que lo transporta. */
final class EventEnvelope {

    private EventEnvelope() {
    }

    static Map<String, Object> sobre(String eventId, String tipo, Instant ocurridoEn, Object datos) {
        Map<String, Object> sobre = new LinkedHashMap<>();
        sobre.put("eventId", eventId);
        sobre.put("type", tipo);
        sobre.put("occurredAt", ocurridoEn.toString());
        sobre.put("data", datos);
        return sobre;
    }

    /** Un mensaje JSON persistente con el cuerpo dado. */
    static Message mensaje(byte[] cuerpo) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setContentEncoding("UTF-8");
        propiedades.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        return MessageBuilder.withBody(cuerpo).andProperties(propiedades).build();
    }
}
