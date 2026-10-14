package co.unicauca.bancopreguntas.notification.infrastructure.messaging;

import co.unicauca.bancopreguntas.notification.application.NotificationApplicationService;
import co.unicauca.bancopreguntas.notification.domain.ObservacionDeRevisor;
import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de entrada de mensajería (suscriptor): traduce los eventos de los servicios de preguntas y de
 * revisión a casos de uso. Lee el sobre como JSON genérico, sin depender de las clases de otros servicios.
 */
@Component
public class EventsListener {

    private final NotificationApplicationService notificaciones;
    private final IncomingEventParser lector;

    public EventsListener(NotificationApplicationService notificaciones, IncomingEventParser lector) {
        this.notificaciones = notificaciones;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_SUBMITTED)
    public void alEnviarseUnaPregunta(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        notificaciones.alEnviarseUnaPregunta(e.eventId(), e.requireText("questionId"), e.requireText("authorId"),
                e.optionalText("title"));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_PUBLISHED)
    public void alPublicarseUnaPregunta(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        notificaciones.alPublicarseUnaPregunta(e.eventId(), e.requireText("questionId"));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_REVIEWERS_ASSIGNED)
    public void alAsignarseRevisores(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        notificaciones.alAsignarseRevisores(e.eventId(), e.requireText("questionId"), e.optionalInt("round", 1),
                e.requireTextList("reviewerIds"), e.data().path("automatic").asBoolean(false));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_REVIEW_COMPLETED)
    public void alCompletarseUnaRevision(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        String resultado = e.requireText("result");
        boolean aprobada = switch (resultado) {
            case "APPROVED" -> true;
            case "REJECTED" -> false;
            default -> throw new AmqpRejectAndDontRequeueException("Resultado de revisión desconocido: " + resultado);
        };
        notificaciones.alCompletarseUnaRevision(e.eventId(), e.requireText("questionId"), e.requireText("authorId"),
                e.optionalInt("round", 1), aprobada, observaciones(e.data().path("observations")));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_EVALUATION_SUBMITTED)
    public void alEnviarseUnaEvaluacion(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        notificaciones.alEnviarseUnaEvaluacion(e.eventId(), e.requireText("questionId"), e.requireText("authorId"),
                e.optionalInt("round", 1), e.requireText("reviewerId"), e.optionalText("decision"),
                e.optionalText("observation"), e.data().path("late").asBoolean(false));
    }

    private static List<ObservacionDeRevisor> observaciones(JsonNode nodo) {
        List<ObservacionDeRevisor> lista = new ArrayList<>();
        if (nodo.isArray()) {
            nodo.forEach(o -> lista.add(new ObservacionDeRevisor(o.path("reviewerId").asText(""),
                    o.path("decision").asText(""), o.path("text").isTextual() ? o.path("text").asText() : null)));
        }
        return lista;
    }
}
