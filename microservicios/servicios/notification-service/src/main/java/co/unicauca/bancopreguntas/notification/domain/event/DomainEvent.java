package co.unicauca.bancopreguntas.notification.domain.event;

/**
 * Algo relevante que ocurrió en las notificaciones y que interesa a otros servicios. {@link #type()} es también la
 * routing key con la que se publica en el broker.
 */
public sealed interface DomainEvent permits NotificationCreated {

    String type();
}
