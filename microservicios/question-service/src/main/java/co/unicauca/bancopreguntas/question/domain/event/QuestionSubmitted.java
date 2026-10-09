package co.unicauca.bancopreguntas.question.domain.event;

/**
 * El autor envió una pregunta a revisión (HU-02). {@code version} es la versión del contenido que se
 * envía; el servicio de revisión la usa para numerar la ronda.
 */
public record QuestionSubmitted(String questionId, String authorId, String title, String competency, int version)
        implements DomainEvent {

    public static final String TYPE = "question.submitted";

    @Override
    public String type() {
        return TYPE;
    }
}
