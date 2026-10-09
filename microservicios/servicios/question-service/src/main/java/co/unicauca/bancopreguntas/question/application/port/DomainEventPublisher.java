package co.unicauca.bancopreguntas.question.application.port;

import co.unicauca.bancopreguntas.question.domain.event.DomainEvent;

/** Puerto de salida: publicación de eventos de dominio hacia otros servicios. */
public interface DomainEventPublisher {

    void publicar(DomainEvent evento);
}
