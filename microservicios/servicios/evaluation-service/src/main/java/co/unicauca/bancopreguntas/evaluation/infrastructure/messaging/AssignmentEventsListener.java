package co.unicauca.bancopreguntas.evaluation.infrastructure.messaging;

import co.unicauca.bancopreguntas.evaluation.application.EvaluationApplicationService;
import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Adaptador de entrada de mensajería: escucha a assignment-service (qué revisores tiene cada ronda) y lo traduce a un
 * caso de uso. Lee el sobre como JSON genérico, sin depender de las clases de otro servicio.
 */
@Component
public class AssignmentEventsListener {

    private final EvaluationApplicationService evaluaciones;
    private final IncomingEventParser lector;

    public AssignmentEventsListener(EvaluationApplicationService evaluaciones, IncomingEventParser lector) {
        this.evaluaciones = evaluaciones;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_REVIEWERS_ASSIGNED)
    public void alAsignarseRevisores(Message mensaje) {
        IncomingEvent evento = lector.leer(mensaje);
        evaluaciones.alAsignarseRevisores(evento.eventId(), evento.requireText("questionId"),
                evento.requireText("authorId"), evento.optionalText("title"), evento.optionalText("competency"),
                evento.optionalInt("questionVersion", 1), evento.optionalInt("round", 1),
                evento.requireTextList("reviewerIds"), evento.optionalText("assignedBy") == null
                        ? "system" : evento.optionalText("assignedBy"),
                evento.data().path("automatic").asBoolean(false), instante(evento.occurredAt()));
    }

    private static Instant instante(String texto) {
        try {
            return texto == null ? null : Instant.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
