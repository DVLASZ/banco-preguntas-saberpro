package co.unicauca.bancopreguntas.question.infrastructure.messaging;

import co.unicauca.bancopreguntas.platform.events.EventPublisher;
import co.unicauca.bancopreguntas.question.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.question.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

/**
 * Adaptador de mensajería: publica los eventos de dominio de preguntas con el publicador común. El tipo del
 * evento es la routing key y el evento mismo, el cuerpo {@code data} (patrón Adapter).
 */
@Component
public class QuestionEventPublisher implements DomainEventPublisher {

    private final EventPublisher publicador;

    public QuestionEventPublisher(EventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(DomainEvent evento) {
        publicador.publicar(evento.type(), evento);
    }
}
