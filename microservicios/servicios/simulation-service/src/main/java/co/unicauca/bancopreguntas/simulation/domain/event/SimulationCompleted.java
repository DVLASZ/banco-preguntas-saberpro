package co.unicauca.bancopreguntas.simulation.domain.event;

import java.time.Instant;
import java.util.List;

/**
 * Un simulacro terminó, porque el estudiante lo finalizó o porque se agotó el tiempo, y ya está calificado. Lo
 * consume el seguimiento académico para el historial y las estadísticas.
 *
 * @param expired {@code true} si lo cerró el sistema por tiempo agotado
 * @param score   porcentaje de aciertos, de 0 a 100
 */
public record SimulationCompleted(String simulationId, String studentId, Instant startedAt, Instant finishedAt,
                                  long durationSeconds, boolean expired, int total, int correct, int incorrect,
                                  int unanswered, double score, List<ByCompetency> byCompetency)
        implements DomainEvent {

    public static final String TYPE = "simulation.completed";

    public SimulationCompleted {
        byCompetency = List.copyOf(byCompetency);
    }

    @Override
    public String type() {
        return TYPE;
    }

    /** Aciertos en una competencia. */
    public record ByCompetency(String competency, int correct, int total) {
    }
}
