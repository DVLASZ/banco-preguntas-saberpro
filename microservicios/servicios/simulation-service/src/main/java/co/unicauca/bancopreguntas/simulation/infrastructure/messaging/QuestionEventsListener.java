package co.unicauca.bancopreguntas.simulation.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.simulation.application.SimulationApplicationService;
import co.unicauca.bancopreguntas.simulation.domain.PreguntaDeCatalogo;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Adaptador de entrada de mensajería: escucha los eventos del servicio de preguntas y los traduce a casos de
 * uso. Lee el sobre como JSON genérico, sin depender de las clases de otro servicio.
 */
@Component
public class QuestionEventsListener {

    private final SimulationApplicationService simulacros;
    private final IncomingEventParser lector;

    public QuestionEventsListener(SimulationApplicationService simulacros, IncomingEventParser lector) {
        this.simulacros = simulacros;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_PUBLISHED)
    public void alPublicarseUnaPregunta(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        String respuesta = e.requireText("correctAnswer");
        PreguntaDeCatalogo pregunta;
        try {
            pregunta = new PreguntaDeCatalogo(e.requireText("questionId"), e.optionalInt("version", 1),
                    e.optionalText("title"), e.requireText("competency"), e.optionalText("topic"),
                    e.optionalText("subtopic"), e.optionalText("difficulty"), e.optionalText("context"),
                    e.requireText("statement"), e.requireText("optionA"), e.requireText("optionB"),
                    e.requireText("optionC"), e.requireText("optionD"), respuesta.charAt(0), instante(e.occurredAt()),
                    true);
        } catch (IllegalArgumentException ex) {
            // un mensaje malformado no se arregla reintentándolo
            throw new AmqpRejectAndDontRequeueException("Pregunta publicada inválida: " + ex.getMessage(), ex);
        }
        simulacros.alPublicarseUnaPregunta(e.eventId(), pregunta);
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_STATE_CHANGED)
    public void alCambiarElEstadoDeUnaPregunta(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        simulacros.alCambiarElEstadoDeUnaPregunta(e.eventId(), e.requireText("questionId"), e.requireText("to"));
    }

    private static Instant instante(String texto) {
        try {
            return texto == null ? null : Instant.parse(texto);
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }
}
