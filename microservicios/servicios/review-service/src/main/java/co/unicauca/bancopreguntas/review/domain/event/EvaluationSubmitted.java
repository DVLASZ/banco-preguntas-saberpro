package co.unicauca.bancopreguntas.review.domain.event;

/**
 * Un revisor envió su evaluación. {@code late} indica que llegó después de que la ronda ya se había rechazado:
 * no cambia el resultado, pero el autor debe enterarse.
 */
public record EvaluationSubmitted(String questionId, String authorId, int round, String reviewerId,
                                  String decision, String observation, boolean late) implements DomainEvent {

    public static final String TYPE = "review.evaluation-submitted";

    @Override
    public String type() {
        return TYPE;
    }
}
