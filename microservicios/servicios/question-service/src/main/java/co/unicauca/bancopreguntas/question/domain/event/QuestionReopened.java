package co.unicauca.bancopreguntas.question.domain.event;

/**
 * El autor reabrió una pregunta rechazada para corregirla; vuelve a Borrador como una versión nueva.
 * El servicio de revisión cierra con este evento la ronda que estaba abierta a observaciones.
 */
public record QuestionReopened(String questionId, String authorId, int version) implements DomainEvent {

    public static final String TYPE = "question.reopened";

    @Override
    public String type() {
        return TYPE;
    }
}
