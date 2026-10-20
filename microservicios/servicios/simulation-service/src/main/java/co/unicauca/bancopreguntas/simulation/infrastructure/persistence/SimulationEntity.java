package co.unicauca.bancopreguntas.simulation.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Fila de {@code simulation}: un simulacro con su calificación. */
@Entity
@Table(name = "simulation")
class SimulationEntity {

    @Id
    @Column(length = 36)
    String id;

    @Column(name = "student_id", nullable = false, length = 100)
    String studentId;

    @Column(nullable = false, length = 20)
    String status;

    @Column(name = "started_at", nullable = false)
    Instant startedAt;

    @Column(name = "due_at", nullable = false)
    Instant dueAt;

    @Column(name = "finished_at")
    Instant finishedAt;

    @Column(name = "duration_minutes", nullable = false)
    int durationMinutes;

    @Column(name = "requested_count", nullable = false)
    int requestedCount;

    /** Criterios entre barras ({@code |LECTURA_CRITICA|INGLES|}); {@code |} si no se filtró por ese criterio. */
    @Column(nullable = false, length = 500)
    String competencies = "|";

    @Column(nullable = false, length = 1000)
    String topics = "|";

    @Column(nullable = false, length = 200)
    String difficulties = "|";

    Integer total;
    Integer correct;
    Integer incorrect;
    Integer unanswered;

    @Column(precision = 5, scale = 2)
    BigDecimal score;

    /** Aciertos por competencia en JSON. */
    @Column(name = "by_competency", columnDefinition = "text")
    String byCompetency;

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    List<SimulationItemEntity> items = new ArrayList<>();

    protected SimulationEntity() {
        // requerido por JPA
    }
}
