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

/** Fila de {@code evaluation}: la evaluación, definitiva, de un revisor en una ronda. */
@Entity
@Table(name = "evaluation")
public class EvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private ReviewRoundEntity round;

    @Column(name = "reviewer_id", nullable = false)
    private String reviewerId;

    @Column(nullable = false)
    private String decision;

    @Column
    private String observation;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private boolean late;

    protected EvaluationEntity() {
        // requerido por JPA
    }

    EvaluationEntity(ReviewRoundEntity round, String reviewerId, String decision, String observation,
                     Instant submittedAt, boolean late) {
        this.round = round;
        this.reviewerId = reviewerId;
        this.decision = decision;
        this.observation = observation;
        this.submittedAt = submittedAt;
        this.late = late;
    }

    public Long getId() { return id; }
    public ReviewRoundEntity getRound() { return round; }
    public String getReviewerId() { return reviewerId; }
    public String getDecision() { return decision; }
    public String getObservation() { return observation; }
    public Instant getSubmittedAt() { return submittedAt; }
    public boolean isLate() { return late; }
}
