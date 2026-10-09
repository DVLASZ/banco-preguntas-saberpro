package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de {@code question_state_history}: una entrada del historial de estados. */
@Entity
@Table(name = "question_state_history")
public class StateHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    private String desde;

    @Column(nullable = false)
    private String hacia;

    @Column(name = "cambiado_por")
    private String cambiadoPor;

    @Column(nullable = false)
    private Instant fecha;

    protected StateHistoryEntity() {
        // requerido por JPA
    }

    public StateHistoryEntity(String questionId, String desde, String hacia, String cambiadoPor, Instant fecha) {
        this.questionId = questionId;
        this.desde = desde;
        this.hacia = hacia;
        this.cambiadoPor = cambiadoPor;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public String getQuestionId() { return questionId; }
    public String getDesde() { return desde; }
    public String getHacia() { return hacia; }
    public String getCambiadoPor() { return cambiadoPor; }
    public Instant getFecha() { return fecha; }
}
