package co.unicauca.bancopreguntas.simulation.domain;

import java.util.List;

/**
 * Calificación automática de un simulacro (RF-24).
 *
 * @param puntaje porcentaje de aciertos sobre el total de preguntas, de 0 a 100 con dos decimales
 */
public record ResultadoDeSimulacro(int total, int correctas, int incorrectas, int sinResponder, double puntaje,
                                   List<ResultadoPorCompetencia> porCompetencia) {

    public ResultadoDeSimulacro {
        porCompetencia = List.copyOf(porCompetencia);
    }
}
