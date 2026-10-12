package co.unicauca.bancopreguntas.review.domain.event;

/**
 * Algo relevante que ocurrió en la revisión y que interesa a otros servicios. {@link #type()} es también la
 * routing key con la que se publica en el broker.
 */
public sealed interface DomainEvent permits ReviewersAssigned, EvaluationSubmitted, ReviewCompleted {

    String type();
}
