package co.unicauca.bancopreguntas.tracking.api.dto;

import co.unicauca.bancopreguntas.tracking.domain.DesempenoPorCompetencia;

/** Desempeño en una competencia. */
public record CompetencyResponse(String competencia, int correctas, int total, double porcentaje) {

    public static CompetencyResponse de(DesempenoPorCompetencia d) {
        return new CompetencyResponse(d.competencia(), d.correctas(), d.total(), d.porcentaje());
    }
}
