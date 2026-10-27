package co.unicauca.bancopreguntas.assignment.domain.event;

/**
 * Algo relevante que ocurrió en la asignación y que interesa a otros servicios. {@link #type()} es también la
 * routing key con la que se publica en el broker.
 */
public sealed interface DomainEvent permits ReviewersAssigned {

    String type();
}
