package co.unicauca.bancopreguntas.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila de {@code question_info}. */
@Entity
@Table(name = "question_info")
public class QuestionInfoEntity {

    @Id
    @Column(name = "question_id", length = 36)
    private String questionId;

    @Column
    private String title;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(name = "admin_id")
    private String adminId;

    protected QuestionInfoEntity() {
        // requerido por JPA
    }

    QuestionInfoEntity(String questionId, String title, String authorId) {
        this.questionId = questionId;
        this.title = title;
        this.authorId = authorId;
    }

    public String getQuestionId() { return questionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
}
