package co.unicauca.bancopreguntas.assignment.application.port;

import co.unicauca.bancopreguntas.assignment.domain.event.DomainEvent;

/** Puerto de salida: publicación de eventos de dominio hacia otros servicios. */
public interface DomainEventPublisher {

    void publicar(DomainEvent evento);
}
