package co.unicauca.bancopreguntas.simulation.application;

import co.unicauca.bancopreguntas.simulation.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.simulation.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/** Doble de prueba que recuerda los eventos publicados. */
class RecordingEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> publicados = new ArrayList<>();

    @Override
    public void publicar(DomainEvent evento) {
        publicados.add(evento);
    }

    List<DomainEvent> publicados() {
        return List.copyOf(publicados);
    }
}
