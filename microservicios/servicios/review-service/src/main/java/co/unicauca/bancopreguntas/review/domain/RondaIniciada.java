package co.unicauca.bancopreguntas.review.domain;

/**
 * Resultado de pedir una ronda para una versión de la pregunta.
 *
 * @param ronda la ronda iniciada, o la que ya existía para esa versión
 * @param nueva {@code false} si la ronda ya existía (un evento repetido), {@code true} si se creó ahora
 */
public record RondaIniciada(RondaDeRevision ronda, boolean nueva) {

    /** Si los revisores de la ronda anterior se reasignaron solos y la ronda ya está en curso. */
    public boolean conRevisoresReasignados() {
        return nueva && ronda.estado() == EstadoRonda.EN_CURSO;
    }
}
