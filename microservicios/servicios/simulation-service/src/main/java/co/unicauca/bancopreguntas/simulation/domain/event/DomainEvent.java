package co.unicauca.bancopreguntas.simulation.domain.event;

/**
 * Algo relevante que ocurrió en los simulacros y que interesa a otros servicios. {@link #type()} es también la
 * routing key con la que se publica en el broker.
 */
public sealed interface DomainEvent permits SimulationCompleted {

    String type();
}
