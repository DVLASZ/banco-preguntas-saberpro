package co.unicauca.bancopreguntas.question.domain.event;

/**
 * Algo relevante que ocurrió en el dominio de preguntas y que interesa a otros servicios.
 * {@link #type()} es también la routing key con la que se publica en el broker.
 */
public sealed interface DomainEvent permits QuestionSubmitted, QuestionStateChanged, QuestionReopened, QuestionPublished {

    String type();
}
