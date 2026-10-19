package co.unicauca.bancopreguntas.tracking.domain;

import java.time.Instant;
import java.util.List;

/**
 * Lo que este servicio recuerda de un simulacro terminado (RF-25): una entrada del historial del estudiante. Se
 * aprende del evento {@code simulation.completed}.
 *
 * @param expirado {@code true} si el simulacro se cerró por tiempo agotado
 * @param puntaje  porcentaje de aciertos sobre todas las preguntas, de 0 a 100
 */
public record ResultadoRegistrado(String simulacroId, String estudianteId, Instant iniciadoEn, Instant finalizadoEn,
                                  long duracionSegundos, boolean expirado, int total, int correctas, int incorrectas,
                                  int sinResponder, double puntaje, List<ResultadoPorCompetencia> porCompetencia) {

    public ResultadoRegistrado {
        if (simulacroId == null || simulacroId.isBlank() || estudianteId == null || estudianteId.isBlank()) {
            throw new IllegalArgumentException("El resultado necesita simulacro y estudiante");
        }
        if (finalizadoEn == null) {
            throw new IllegalArgumentException("El resultado necesita la fecha en que terminó el simulacro");
        }
        if (total < 1 || correctas < 0 || correctas > total) {
            throw new IllegalArgumentException("Los aciertos deben estar entre 0 y el total de preguntas");
        }
        porCompetencia = porCompetencia == null ? List.of() : List.copyOf(porCompetencia);
    }
}
