package co.unicauca.bancopreguntas.tracking.domain;

import java.time.Instant;
import java.util.List;

/** Resultados de prueba. */
public final class Resultados {

    public static final Instant T0 = Instant.parse("2026-10-16T10:00:00Z");

    private Resultados() {
    }

    /** Un simulacro de {@code total} preguntas terminado {@code dias} días después de T0. */
    public static ResultadoRegistrado resultado(String id, String estudiante, int dias, int correctas, int total,
                                                ResultadoPorCompetencia... competencias) {
        double puntaje = Math.round(correctas * 10000.0 / total) / 100.0;
        Instant fin = T0.plusSeconds(dias * 86400L);
        return new ResultadoRegistrado(id, estudiante, fin.minusSeconds(600), fin, 600, false, total, correctas,
                total - correctas, 0, puntaje, List.of(competencias));
    }

    public static ResultadoPorCompetencia comp(String competencia, int correctas, int total) {
        return new ResultadoPorCompetencia(competencia, correctas, total);
    }
}
