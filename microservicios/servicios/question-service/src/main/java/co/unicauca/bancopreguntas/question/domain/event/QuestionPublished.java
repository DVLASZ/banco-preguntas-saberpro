package co.unicauca.bancopreguntas.question.domain.event;

/** El administrador publicó una pregunta aprobada; ya puede usarse en simulacros. */
public record QuestionPublished(String questionId, String publishedBy) implements DomainEvent {

    public static final String TYPE = "question.published";

    @Override
    public String type() {
        return TYPE;
    }
}
