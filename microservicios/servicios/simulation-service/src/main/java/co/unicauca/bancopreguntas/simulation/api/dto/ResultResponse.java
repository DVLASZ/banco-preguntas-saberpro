package co.unicauca.bancopreguntas.simulation.api.dto;

import co.unicauca.bancopreguntas.simulation.domain.ResultadoDeSimulacro;

import java.util.List;

/** La calificación de un simulacro (RF-24). */
public record ResultResponse(int total, int correctas, int incorrectas, int sinResponder, double puntaje,
                             List<CompetencyResult> porCompetencia) {

    public static ResultResponse de(ResultadoDeSimulacro r) {
        return new ResultResponse(r.total(), r.correctas(), r.incorrectas(), r.sinResponder(), r.puntaje(),
                r.porCompetencia().stream().map(c -> new CompetencyResult(c.competencia(), c.correctas(), c.total(),
                        c.total() == 0 ? 0 : Math.round(c.correctas() * 10000.0 / c.total()) / 100.0)).toList());
    }

    /** Aciertos en una competencia, con su porcentaje. */
    public record CompetencyResult(String competencia, int correctas, int total, double porcentaje) {
    }
}
