package co.unicauca.bancopreguntas.evaluation.domain.event;

/**
 * Algo relevante que ocurrió en la evaluación y que interesa a otros servicios. {@link #type()} es también la
 * routing key con la que se publica en el broker.
 */
public sealed interface DomainEvent permits EvaluationSubmitted, ReviewCompleted {

    String type();
}
