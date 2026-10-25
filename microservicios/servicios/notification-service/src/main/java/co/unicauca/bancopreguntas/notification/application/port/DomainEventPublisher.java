package co.unicauca.bancopreguntas.notification.application.port;

import co.unicauca.bancopreguntas.notification.domain.event.DomainEvent;

/** Puerto de salida: publicación de eventos de dominio hacia otros servicios. */
public interface DomainEventPublisher {

    void publicar(DomainEvent evento);
}
