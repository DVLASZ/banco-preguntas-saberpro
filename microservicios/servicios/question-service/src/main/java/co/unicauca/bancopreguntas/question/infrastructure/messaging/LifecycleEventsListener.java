package co.unicauca.bancopreguntas.question.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.question.application.QuestionApplicationService;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada de mensajería: escucha los cambios de estado que publica lifecycle-service y los traduce a un
 * caso de uso. Lee el sobre como JSON genérico, sin depender de las clases de otros servicios.
 */
@Component
public class LifecycleEventsListener {

    private final QuestionApplicationService preguntas;
    private final IncomingEventParser lector;

    public LifecycleEventsListener(QuestionApplicationService preguntas, IncomingEventParser lector) {
        this.preguntas = preguntas;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_STATE_CHANGED)
    public void alCambiarElEstado(Message mensaje) {
        IncomingEvent evento = lector.leer(mensaje);
        String hacia = evento.requireText("to");
        EstadoPregunta estado;
        try {
            estado = EstadoPregunta.valueOf(hacia);
        } catch (IllegalArgumentException e) {
            // un estado desconocido no se arregla reintentándolo
            throw new AmqpRejectAndDontRequeueException("Estado desconocido: " + hacia, e);
        }
        preguntas.alCambiarElEstado(evento.requireText("questionId"), estado, evento.optionalInt("version", 1),
                evento.optionalInt("sequence", 0));
    }
}
