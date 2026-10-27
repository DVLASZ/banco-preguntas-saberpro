package co.unicauca.bancopreguntas.assignment.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Fila de {@code review_round}. */
@Entity
@Table(name = "review_round")
public class RoundEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseEntity assignmentCase;

    @Column(nullable = false)
    private int number;

    @Column(name = "question_version", nullable = false)
    private int questionVersion;

    @Column(nullable = false)
    private String state;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("assignedAt ASC, id ASC")
    private List<AssignmentEntity> assignments = new ArrayList<>();

    protected RoundEntity() {
        // requerido por JPA
    }

    RoundEntity(String id, CaseEntity assignmentCase) {
        this.id = id;
        this.assignmentCase = assignmentCase;
    }

    public String getId() { return id; }
    public CaseEntity getAssignmentCase() { return assignmentCase; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public int getQuestionVersion() { return questionVersion; }
    public void setQuestionVersion(int questionVersion) { this.questionVersion = questionVersion; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public List<AssignmentEntity> getAssignments() { return assignments; }
}
