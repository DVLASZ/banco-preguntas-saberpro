package co.unicauca.bancopreguntas.lifecycle.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.lifecycle.application.LifecycleApplicationService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada de mensajería (la mitad "suscriptor"): escucha los eventos del servicio de revisión
 * y los traduce a casos de uso. Lee el sobre como JSON genérico, sin depender de las clases de otros servicios.
 */
@Component
public class ReviewEventsListener {

    private final LifecycleApplicationService ciclos;
    private final IncomingEventParser lector;

    public ReviewEventsListener(LifecycleApplicationService ciclos, IncomingEventParser lector) {
        this.ciclos = ciclos;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_REVIEWERS_ASSIGNED)
    public void alAsignarRevisores(Message mensaje) {
        IncomingEvent evento = lector.leer(mensaje);
        ciclos.aplicarRevisoresAsignados(evento.eventId(), evento.requireText("questionId"));
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
        ciclos.aplicarRevisionCompletada(evento.eventId(), evento.requireText("questionId"), aprobada);
    }
}
