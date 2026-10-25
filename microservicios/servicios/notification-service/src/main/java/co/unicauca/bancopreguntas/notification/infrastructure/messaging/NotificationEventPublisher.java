package co.unicauca.bancopreguntas.notification.infrastructure.messaging;

import co.unicauca.bancopreguntas.notification.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.notification.domain.event.DomainEvent;
import co.unicauca.bancopreguntas.platform.events.EventPublisher;
import org.springframework.stereotype.Component;

/** Adaptador de mensajería: publica los eventos de las notificaciones con el publicador común (patrón Adapter). */
@Component
public class NotificationEventPublisher implements DomainEventPublisher {

    private final EventPublisher publicador;

    public NotificationEventPublisher(EventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(DomainEvent evento) {
        publicador.publicar(evento.type(), evento);
    }
}
