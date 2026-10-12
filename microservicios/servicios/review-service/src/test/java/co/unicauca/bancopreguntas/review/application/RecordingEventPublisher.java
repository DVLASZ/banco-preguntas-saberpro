package co.unicauca.bancopreguntas.review.application;

import co.unicauca.bancopreguntas.review.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.review.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/** Doble de prueba del puerto de eventos: recuerda lo que se publicó, en orden. */
class RecordingEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> publicados = new ArrayList<>();

    @Override
    public void publicar(DomainEvent evento) {
        publicados.add(evento);
    }

    List<DomainEvent> publicados() {
        return List.copyOf(publicados);
    }

    List<String> tipos() {
        return publicados.stream().map(DomainEvent::type).toList();
    }
}
