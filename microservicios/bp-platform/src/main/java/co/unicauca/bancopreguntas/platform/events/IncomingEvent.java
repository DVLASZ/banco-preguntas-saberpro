package co.unicauca.bancopreguntas.platform.events;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.ArrayList;
import java.util.List;

/**
 * Un evento recibido, leído como JSON genérico: el consumidor no depende de las clases del servicio que lo
 * publicó. Los métodos {@code require*} descartan el mensaje (sin reencolarlo) si le falta un dato, porque
 * un mensaje malformado no se arregla reintentándolo.
 */
public record IncomingEvent(String eventId, String type, String occurredAt, JsonNode data) {

    private static final Logger LOG = LoggerFactory.getLogger(IncomingEvent.class);

    /** Texto obligatorio dentro del cuerpo {@code data}. */
    public String requireText(String campo) {
        JsonNode valor = data.path(campo);
        if (!valor.isTextual() || valor.asText().isBlank()) {
            throw descartar("Falta el campo " + campo + " en el evento " + type);
        }
        return valor.asText();
    }

    /** Texto opcional dentro del cuerpo {@code data}; {@code null} si falta o es nulo. */
    public String optionalText(String campo) {
        JsonNode valor = data.path(campo);
        return valor.isTextual() && !valor.asText().isBlank() ? valor.asText() : null;
    }

    /** Entero obligatorio dentro del cuerpo {@code data}. */
    public int requireInt(String campo) {
        JsonNode valor = data.path(campo);
        if (!valor.isInt()) {
            throw descartar("Falta el campo numérico " + campo + " en el evento " + type);
        }
        return valor.asInt();
    }

    /** Entero opcional dentro del cuerpo {@code data}, con un valor por defecto. */
    public int optionalInt(String campo, int porDefecto) {
        JsonNode valor = data.path(campo);
        return valor.isInt() ? valor.asInt() : porDefecto;
    }

    /** Lista de textos obligatoria (puede estar vacía) dentro del cuerpo {@code data}. */
    public List<String> requireTextList(String campo) {
        JsonNode valor = data.path(campo);
        if (!valor.isArray()) {
            throw descartar("Falta la lista " + campo + " en el evento " + type);
        }
        List<String> textos = new ArrayList<>();
        valor.forEach(e -> {
            if (!e.isTextual() || e.asText().isBlank()) {
                throw descartar("La lista " + campo + " del evento " + type + " tiene un elemento inválido");
            }
            textos.add(e.asText());
        });
        return textos;
    }

    static AmqpRejectAndDontRequeueException descartar(String motivo) {
        LOG.error("Mensaje descartado: {}", motivo);
        return new AmqpRejectAndDontRequeueException(motivo);
    }
}
