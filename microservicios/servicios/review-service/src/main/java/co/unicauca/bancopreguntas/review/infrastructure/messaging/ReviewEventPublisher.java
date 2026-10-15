package co.unicauca.bancopreguntas.review.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventPublisher;
import co.unicauca.bancopreguntas.review.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.review.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

/** Adaptador de mensajería: publica los eventos de la revisión con el publicador común (patrón Adapter). */
@Component
public class ReviewEventPublisher implements DomainEventPublisher {

    private final EventPublisher publicador;

    public ReviewEventPublisher(EventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(DomainEvent evento) {
        publicador.publicar(evento.type(), evento);
    }
}
