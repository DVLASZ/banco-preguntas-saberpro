package co.unicauca.bancopreguntas.evaluation.domain.event;

import java.util.List;

/**
 * La ronda tomó una decisión. {@code result} es {@code APPROVED} o {@code REJECTED}; {@code observations}
 * son las evaluaciones recibidas hasta ese momento (las tardías llegan después con
 * {@link EvaluationSubmitted}).
 */
public record ReviewCompleted(String questionId, String authorId, int round, String result,
                              List<Observation> observations) implements DomainEvent {

    public static final String TYPE = "review.completed";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    public ReviewCompleted {
        observations = List.copyOf(observations);
    }

    @Override
    public String type() {
        return TYPE;
    }

    /** La evaluación de un revisor incluida en el resultado. */
    public record Observation(String reviewerId, String decision, String text) {
    }
}
