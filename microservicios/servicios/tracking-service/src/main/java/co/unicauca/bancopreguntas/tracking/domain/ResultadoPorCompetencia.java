package co.unicauca.bancopreguntas.tracking.domain;

/** Aciertos de un simulacro en una competencia. */
public record ResultadoPorCompetencia(String competencia, int correctas, int total) {

    public ResultadoPorCompetencia {
        if (competencia == null || competencia.isBlank() || correctas < 0 || total < correctas) {
            throw new IllegalArgumentException("El resultado por competencia no es válido");
        }
    }
}
