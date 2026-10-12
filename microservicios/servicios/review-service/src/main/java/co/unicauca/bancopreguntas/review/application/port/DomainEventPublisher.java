package co.unicauca.bancopreguntas.review.application.port;

import co.unicauca.bancopreguntas.review.domain.event.DomainEvent;

/** Puerto de salida: publicación de eventos de dominio hacia otros servicios. */
public interface DomainEventPublisher {

    void publicar(DomainEvent evento);
}
