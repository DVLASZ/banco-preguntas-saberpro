package co.unicauca.bancopreguntas.assignment.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Fila de {@code review_case}. Es un detalle de persistencia: el dominio usa {@code CasoDeAsignacion}. */
@Entity
@Table(name = "review_case")
public class CaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "question_id", nullable = false, unique = true)
    private String questionId;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String competency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "assignmentCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    private List<RoundEntity> rounds = new ArrayList<>();

    protected CaseEntity() {
        // requerido por JPA
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompetency() { return competency; }
    public void setCompetency(String competency) { this.competency = competency; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<RoundEntity> getRounds() { return rounds; }
}
