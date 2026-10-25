package co.unicauca.bancopreguntas.notification.application;

import co.unicauca.bancopreguntas.notification.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.notification.domain.event.DomainEvent;
import co.unicauca.bancopreguntas.notification.domain.event.NotificationCreated;

import java.util.ArrayList;
import java.util.List;

/** Doble de prueba de la publicación de eventos: recuerda lo publicado. */
class RecordingEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> publicados = new ArrayList<>();

    @Override
    public void publicar(DomainEvent evento) {
        publicados.add(evento);
    }

    List<DomainEvent> publicados() {
        return publicados;
    }

    List<NotificationCreated> notificaciones() {
        return publicados.stream().filter(NotificationCreated.class::isInstance).map(NotificationCreated.class::cast).toList();
    }
}
