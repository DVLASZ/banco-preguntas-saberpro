package co.unicauca.bancopreguntas.simulation.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventPublisher;
import co.unicauca.bancopreguntas.simulation.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.simulation.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

/** Adaptador de mensajería: publica los eventos de los simulacros con el publicador común (patrón Adapter). */
@Component
public class SimulationEventPublisher implements DomainEventPublisher {

    private final EventPublisher publicador;

    public SimulationEventPublisher(EventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(DomainEvent evento) {
        publicador.publicar(evento.type(), evento);
    }
}
