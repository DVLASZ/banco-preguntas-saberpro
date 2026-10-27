package co.unicauca.bancopreguntas.evaluation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de {@code round_assignment}: un revisor asignado a una ronda. */
@Entity
@Table(name = "round_assignment")
public class AssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private ReviewRoundEntity round;

    @Column(name = "reviewer_id", nullable = false)
    private String reviewerId;

    @Column(name = "assigned_by", nullable = false)
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(nullable = false)
    private boolean automatic;

    protected AssignmentEntity() {
        // requerido por JPA
    }

    AssignmentEntity(ReviewRoundEntity round, String reviewerId, String assignedBy, Instant assignedAt,
                     boolean automatic) {
        this.round = round;
        this.reviewerId = reviewerId;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
        this.automatic = automatic;
    }

    public Long getId() { return id; }
    public ReviewRoundEntity getRound() { return round; }
    public String getReviewerId() { return reviewerId; }
    public String getAssignedBy() { return assignedBy; }
    public Instant getAssignedAt() { return assignedAt; }
    public boolean isAutomatic() { return automatic; }
}
