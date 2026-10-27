package co.unicauca.bancopreguntas.evaluation.api.dto;

import co.unicauca.bancopreguntas.evaluation.domain.ResultadoDeEvaluacion;

/**
 * Lo que produjo enviar una evaluación. {@code resultadoDeLaRonda} es {@code APROBADA} o {@code RECHAZADA} si
 * esta evaluación decidió la ronda y {@code PENDIENTE} si no (faltan evaluaciones, o fue una observación tardía).
 */
public record EvaluationResultResponse(String resultadoDeLaRonda, boolean tardia, int ronda) {

    public static EvaluationResultResponse de(ResultadoDeEvaluacion r) {
        return new EvaluationResultResponse(r.decision().name(), r.evaluacion().tardia(), r.ronda().numero());
    }
}
