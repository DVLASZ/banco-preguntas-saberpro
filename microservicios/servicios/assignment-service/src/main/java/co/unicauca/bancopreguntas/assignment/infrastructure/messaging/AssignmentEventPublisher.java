package co.unicauca.bancopreguntas.assignment.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventPublisher;
import co.unicauca.bancopreguntas.assignment.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.assignment.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

/** Adaptador de mensajería: publica los eventos de la revisión con el publicador común (patrón Adapter). */
@Component
public class AssignmentEventPublisher implements DomainEventPublisher {

    private final EventPublisher publicador;

    public AssignmentEventPublisher(EventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(DomainEvent evento) {
        publicador.publicar(evento.type(), evento);
    }
}
