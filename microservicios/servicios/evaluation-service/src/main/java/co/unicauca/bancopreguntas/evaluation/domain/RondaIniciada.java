package co.unicauca.bancopreguntas.evaluation.domain;

/**
 * Resultado de pedir una ronda para una versión de la pregunta.
 *
 * @param ronda la ronda iniciada, o la que ya existía para esa versión
 * @param nueva {@code false} si la ronda ya existía (un evento repetido), {@code true} si se creó ahora
 */
public record RondaIniciada(RondaDeEvaluacion ronda, boolean nueva) {
}
