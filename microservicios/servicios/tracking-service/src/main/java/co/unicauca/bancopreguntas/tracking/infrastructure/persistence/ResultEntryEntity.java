package co.unicauca.bancopreguntas.tracking.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Fila de {@code result_entry}: un simulacro terminado en el historial de un estudiante. */
@Entity
@Table(name = "result_entry")
class ResultEntryEntity {

    @Id
    @Column(name = "simulation_id", length = 36)
    String simulationId;

    @Column(name = "student_id", nullable = false, length = 100)
    String studentId;

    @Column(name = "started_at")
    Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    Instant finishedAt;

    @Column(name = "duration_seconds", nullable = false)
    long durationSeconds;

    @Column(nullable = false)
    boolean expired;

    @Column(nullable = false)
    int total;

    @Column(nullable = false)
    int correct;

    @Column(nullable = false)
    int incorrect;

    @Column(nullable = false)
    int unanswered;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal score;

    /** Aciertos por competencia en JSON. */
    @Column(name = "by_competency", nullable = false, columnDefinition = "text")
    String byCompetency;

    protected ResultEntryEntity() {
        // requerido por JPA
    }
}
