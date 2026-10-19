package co.unicauca.bancopreguntas.simulation.application.port;

import co.unicauca.bancopreguntas.simulation.domain.event.DomainEvent;

/** Puerto de salida: publicación de eventos de dominio hacia otros servicios. */
public interface DomainEventPublisher {

    void publicar(DomainEvent evento);
}
