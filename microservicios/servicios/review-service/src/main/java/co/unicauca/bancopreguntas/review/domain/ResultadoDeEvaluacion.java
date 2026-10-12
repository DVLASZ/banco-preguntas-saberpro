package co.unicauca.bancopreguntas.review.domain;

/**
 * Lo que produjo registrar una evaluación.
 *
 * @param evaluacion la evaluación registrada
 * @param ronda      la ronda a la que pertenece
 * @param decision   {@code APROBADA} o {@code RECHAZADA} si <b>esta</b> evaluación decidió la ronda;
 *                   {@code PENDIENTE} si no la decidió (faltan evaluaciones, o era una observación tardía)
 */
public record ResultadoDeEvaluacion(Evaluacion evaluacion, RondaDeRevision ronda, ResultadoDeDecision decision) {

    /** Si esta evaluación cerró la decisión de la ronda. */
    public boolean decidioLaRonda() {
        return decision != ResultadoDeDecision.PENDIENTE;
    }
}
