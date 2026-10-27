package co.unicauca.bancopreguntas.lifecycle.application.port;

import co.unicauca.bancopreguntas.lifecycle.domain.event.DomainEvent;

/** Puerto de salida: publicación de eventos de dominio hacia otros servicios. */
public interface DomainEventPublisher {

    void publicar(DomainEvent evento);
}
