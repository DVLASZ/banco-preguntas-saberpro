package co.unicauca.bancopreguntas.history.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila de {@code question_reviewer}: alguien que es o fue revisor de una pregunta. */
@Entity
@Table(name = "question_reviewer")
public class QuestionReviewerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "reviewer_id", nullable = false)
    private String reviewerId;

    protected QuestionReviewerEntity() {
        // requerido por JPA
    }

    public Long getId() { return id; }
    public String getQuestionId() { return questionId; }
    public String getReviewerId() { return reviewerId; }
}
