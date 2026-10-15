package co.unicauca.bancopreguntas.review.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.review.application.ReviewApplicationService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada de mensajería: escucha los eventos del servicio de preguntas y los traduce a casos
 * de uso. Lee el sobre como JSON genérico, sin depender de las clases de otro servicio.
 */
@Component
public class QuestionEventsListener {

    private final ReviewApplicationService revisiones;
    private final IncomingEventParser lector;

    public QuestionEventsListener(ReviewApplicationService revisiones, IncomingEventParser lector) {
        this.revisiones = revisiones;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_SUBMITTED)
    public void alEnviarseUnaPregunta(Message mensaje) {
        IncomingEvent evento = lector.leer(mensaje);
        revisiones.alEnviarseUnaPregunta(evento.eventId(), evento.requireText("questionId"),
                evento.requireText("authorId"), evento.optionalText("title"),
                evento.optionalText("competency"), evento.optionalInt("version", 1));
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_QUESTION_REOPENED)
    public void alReabrirseUnaPregunta(Message mensaje) {
        IncomingEvent evento = lector.leer(mensaje);
        revisiones.alReabrirseUnaPregunta(evento.eventId(), evento.requireText("questionId"));
    }
}
