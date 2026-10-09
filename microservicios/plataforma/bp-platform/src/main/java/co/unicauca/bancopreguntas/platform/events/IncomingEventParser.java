package co.unicauca.bancopreguntas.platform.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;

import java.io.IOException;

/** Lee el sobre {@code {eventId, type, occurredAt, data}} de un mensaje recibido. */
public class IncomingEventParser {

    private final ObjectMapper json;

    public IncomingEventParser(ObjectMapper json) {
        this.json = json;
    }

    /**
     * @throws org.springframework.amqp.AmqpRejectAndDontRequeueException si el mensaje no es JSON o no
     *         tiene {@code eventId}
     */
    public IncomingEvent leer(Message mensaje) {
        JsonNode sobre;
        try {
            sobre = json.readTree(mensaje.getBody());
        } catch (IOException e) {
            throw IncomingEvent.descartar("Mensaje que no es JSON válido");
        }
        if (sobre == null || !sobre.isObject()) {
            throw IncomingEvent.descartar("El mensaje no es un objeto JSON");
        }
        JsonNode eventId = sobre.path("eventId");
        if (!eventId.isTextual() || eventId.asText().isBlank()) {
            throw IncomingEvent.descartar("Falta el campo eventId");
        }
        return new IncomingEvent(eventId.asText(), sobre.path("type").asText(null),
                sobre.path("occurredAt").asText(null), sobre.path("data"));
    }
}
