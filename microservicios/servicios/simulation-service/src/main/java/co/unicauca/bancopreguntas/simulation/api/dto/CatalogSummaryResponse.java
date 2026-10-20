package co.unicauca.bancopreguntas.simulation.api.dto;

import co.unicauca.bancopreguntas.simulation.domain.ResumenDelCatalogo;

import java.util.Map;

/** Cuántas preguntas publicadas hay para armar simulacros. */
public record CatalogSummaryResponse(int total, Map<String, Integer> porCompetencia, Map<String, Integer> porDificultad) {

    public static CatalogSummaryResponse de(ResumenDelCatalogo r) {
        return new CatalogSummaryResponse(r.total(), r.porCompetencia(), r.porDificultad());
    }
}
