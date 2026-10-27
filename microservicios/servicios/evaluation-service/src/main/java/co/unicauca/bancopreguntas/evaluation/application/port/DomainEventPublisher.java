package co.unicauca.bancopreguntas.evaluation.application.port;

import co.unicauca.bancopreguntas.evaluation.domain.event.DomainEvent;

/** Puerto de salida: publicación de eventos de dominio hacia otros servicios. */
public interface DomainEventPublisher {

    void publicar(DomainEvent evento);
}
