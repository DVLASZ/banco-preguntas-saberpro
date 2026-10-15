package co.unicauca.bancopreguntas.user.application.port;

import co.unicauca.bancopreguntas.user.domain.event.DomainEvent;

/** Puerto de salida: publicación de eventos de seguridad hacia otros servicios. */
public interface DomainEventPublisher {

    void publicar(DomainEvent evento);
}
