package co.unicauca.bancopreguntas.lifecycle.domain.event;

/**
 * Una pregunta cambió de estado. {@code changedBy} es nulo si el cambio lo provocó otro servicio. {@code sequence} es
 * el número de cambios que lleva la pregunta (quien guarda una copia del estado ignora los avisos más viejos) y
 * {@code version} la versión de su contenido.
 */
public record QuestionStateChanged(String questionId, String from, String to, String changedBy, int sequence, int version)
        implements DomainEvent {

    public static final String TYPE = "question.state-changed";

    @Override
    public String type() {
        return TYPE;
    }
}
