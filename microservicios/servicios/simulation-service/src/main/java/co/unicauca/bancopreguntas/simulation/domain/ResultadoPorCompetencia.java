package co.unicauca.bancopreguntas.simulation.domain;

/** Aciertos de un simulacro en una competencia. */
public record ResultadoPorCompetencia(String competencia, int correctas, int total) {
}
