package co.unicauca.bancopreguntas.simulation.domain;

import java.util.Map;

/** Cuántas preguntas publicadas hay disponibles para simulacros, en total y por competencia y dificultad. */
public record ResumenDelCatalogo(int total, Map<String, Integer> porCompetencia, Map<String, Integer> porDificultad) {

    public ResumenDelCatalogo {
        porCompetencia = Map.copyOf(porCompetencia);
        porDificultad = Map.copyOf(porDificultad);
    }
}
