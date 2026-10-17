package co.unicauca.bancopreguntas.user.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventPublisher;
import co.unicauca.bancopreguntas.user.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.user.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

/** Adaptador de mensajería: publica los eventos de seguridad con el publicador común (patrón Adapter). */
@Component
public class UserEventPublisher implements DomainEventPublisher {

    private final EventPublisher publicador;

    public UserEventPublisher(EventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(DomainEvent evento) {
        publicador.publicar(evento.type(), evento);
    }
}
