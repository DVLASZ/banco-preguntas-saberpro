package co.unicauca.bancopreguntas.email.infrastructure.messaging;

import co.unicauca.bancopreguntas.email.application.EmailApplicationService;
import co.unicauca.bancopreguntas.platform.events.IncomingEvent;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada de mensajería (suscriptor): traduce {@code notification.created} a un caso de uso. Lee el
 * sobre como JSON genérico, sin depender de las clases del servicio de notificaciones.
 */
@Component
public class EventsListener {

    private final EmailApplicationService correos;
    private final IncomingEventParser lector;

    public EventsListener(EmailApplicationService correos, IncomingEventParser lector) {
        this.correos = correos;
        this.lector = lector;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_NOTIFICATION_CREATED)
    public void alCrearseUnaNotificacion(Message mensaje) {
        IncomingEvent e = lector.leer(mensaje);
        correos.alCrearseUnaNotificacion(e.eventId(), e.requireText("notificationId"), e.requireText("recipientId"),
                e.requireText("subject"), e.requireText("message"));
    }
}
