package co.unicauca.bancopreguntas.audit.infrastructure.messaging;

import co.unicauca.bancopreguntas.audit.application.AuditApplicationService;
import co.unicauca.bancopreguntas.audit.domain.EventoRecibido;
import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Adaptador de entrada de mensajería (suscriptor): traduce los eventos de los demás servicios a entradas
 * del auditoria. Lee el sobre como JSON genérico, sin depender de las clases de otros servicios.
 */
@Component
public class EventsListener {

    private final AuditApplicationService auditoria;
    private final IncomingEventParser lector;

    public EventsListener(AuditApplicationService auditoria, IncomingEventParser lector) {
        this.auditoria = auditoria;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_SUBMITTED)
    public void alEnviarseUnaPregunta(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        auditoria.alEnviarseUnaPregunta(recibido(e), e.requireText("questionId"), e.requireText("authorId"),
                e.optionalText("title"), e.optionalInt("version", 1));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_REOPENED)
    public void alReabrirseUnaPregunta(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        auditoria.alReabrirseUnaPregunta(recibido(e), e.requireText("questionId"), e.optionalText("authorId"),
                e.optionalInt("version", 1));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_PUBLISHED)
    public void alPublicarseUnaPregunta(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        auditoria.alPublicarseUnaPregunta(recibido(e), e.requireText("questionId"), e.optionalText("publishedBy"));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_STATE_CHANGED)
    public void alCambiarDeEstado(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        auditoria.alCambiarDeEstado(recibido(e), e.requireText("questionId"), e.optionalText("from"),
                e.requireText("to"), e.optionalText("changedBy"));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_REVIEWERS_ASSIGNED)
    public void alAsignarseRevisores(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        auditoria.alAsignarseRevisores(recibido(e), e.requireText("questionId"), e.optionalInt("round", 1),
                e.requireTextList("reviewerIds"), e.optionalText("assignedBy"),
                e.data().path("automatic").asBoolean(false));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_EVALUATION_SUBMITTED)
    public void alEnviarseUnaEvaluacion(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        auditoria.alEnviarseUnaEvaluacion(recibido(e), e.requireText("questionId"), e.optionalInt("round", 1),
                e.requireText("reviewerId"), e.optionalText("decision"), e.data().path("late").asBoolean(false));
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
        auditoria.alCompletarseUnaRevision(recibido(e), e.requireText("questionId"), e.optionalInt("round", 1), aprobada);
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_LOGIN_SUCCEEDED)
    public void alIniciarSesion(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        auditoria.alIniciarSesion(recibido(e), e.requireText("userId"), true);
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_LOGIN_FAILED)
    public void alFallarElInicioDeSesion(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        auditoria.alIniciarSesion(recibido(e), e.requireText("userId"), false);
    }

    private static EventoRecibido recibido(IncomingEvent e) {
        return new EventoRecibido(e.eventId(), cuando(e.occurredAt()), e.data().toString());
    }

    private static Instant cuando(String texto) {
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException | NullPointerException ex) {
            return Instant.now();
        }
    }
}
