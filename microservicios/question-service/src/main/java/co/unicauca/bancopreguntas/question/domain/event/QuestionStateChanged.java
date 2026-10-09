package co.unicauca.bancopreguntas.question.domain.event;

/** Una pregunta cambió de estado. {@code changedBy} es nulo si el cambio lo provocó otro servicio. */
public record QuestionStateChanged(String questionId, String from, String to, String changedBy)
        implements DomainEvent {

    public static final String TYPE = "question.state-changed";

    @Override
    public String type() {
        return TYPE;
    }
}
