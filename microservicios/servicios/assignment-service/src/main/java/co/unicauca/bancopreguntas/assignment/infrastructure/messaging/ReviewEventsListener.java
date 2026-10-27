package co.unicauca.bancopreguntas.assignment.infrastructure.messaging;

import co.unicauca.bancopreguntas.assignment.application.AssignmentApplicationService;
import co.unicauca.bancopreguntas.assignment.domain.Decision;
import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada de mensajería: escucha los eventos de evaluation-service (quién evaluó y cómo terminó la ronda)
 * y los traduce a casos de uso. Lee el sobre como JSON genérico, sin depender de las clases de otro servicio.
 */
@Component
public class ReviewEventsListener {

    private final AssignmentApplicationService asignaciones;
    private final IncomingEventParser lector;

    public ReviewEventsListener(AssignmentApplicationService asignaciones, IncomingEventParser lector) {
        this.asignaciones = asignaciones;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_EVALUATION_SUBMITTED)
    public void alEvaluarseUnaPregunta(Message mensaje) {
        IncomingEvent evento = lector.leer(mensaje);
        String decision = evento.requireText("decision");
        Decision decidida;
        try {
            decidida = Decision.valueOf(decision);
        } catch (IllegalArgumentException e) {
            // una decisión desconocida no se arregla reintentándolo
            throw new AmqpRejectAndDontRequeueException("Decisión desconocida: " + decision, e);
        }
        asignaciones.alEvaluarseUnaPregunta(evento.eventId(), evento.requireText("questionId"),
                evento.optionalInt("round", 1), evento.requireText("reviewerId"), decidida);
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_REVIEW_COMPLETED)
    public void alCompletarseLaRevision(Message mensaje) {
        IncomingEvent evento = lector.leer(mensaje);
        String resultado = evento.requireText("result");
        boolean aprobada = switch (resultado) {
            case "APPROVED" -> true;
            case "REJECTED" -> false;
            default -> throw new AmqpRejectAndDontRequeueException("Resultado de revisión desconocido: " + resultado);
        };
        asignaciones.alCompletarseLaRevision(evento.eventId(), evento.requireText("questionId"),
                evento.optionalInt("round", 1), aprobada);
    }
}
