package co.unicauca.bancopreguntas.notification.infrastructure.persistence;

import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de {@code notification}. Es un detalle de persistencia: el dominio usa {@code Notificacion}. */
@Entity
@Table(name = "notification")
public class NotificationEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(nullable = false)
    private String type;

    @Column(name = "question_id")
    private String questionId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected NotificationEntity() {
        // requerido por JPA
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    static NotificationEntity de(Notificacion n) {
        NotificationEntity e = new NotificationEntity();
        e.copiar(n);
        return e;
    }

    void copiar(Notificacion n) {
        id = n.id();
        recipientId = n.destinatarioId();
        type = n.tipo().name();
        questionId = n.preguntaId();
        subject = n.asunto();
        message = n.mensaje();
        createdAt = n.creadaEn();
        readAt = n.leidaEn();
    }

    Notificacion aDominio() {
        return new Notificacion(id, recipientId, TipoNotificacion.valueOf(type), questionId, subject, message,
                createdAt, readAt);
    }
}
