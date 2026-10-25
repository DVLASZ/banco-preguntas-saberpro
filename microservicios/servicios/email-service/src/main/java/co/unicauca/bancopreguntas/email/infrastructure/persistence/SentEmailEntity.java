package co.unicauca.bancopreguntas.email.infrastructure.persistence;

import co.unicauca.bancopreguntas.email.domain.CorreoEnviado;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de {@code sent_email}. Es un detalle de persistencia: el dominio usa {@code CorreoEnviado}. */
@Entity
@Table(name = "sent_email")
public class SentEmailEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "notification_id", length = 36)
    private String notificationId;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "to_address", nullable = false)
    private String toAddress;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(length = 500)
    private String error;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected SentEmailEntity() {
        // requerido por JPA
    }

    public String getStatus() { return status; }

    public Instant getAttemptedAt() { return attemptedAt; }

    static SentEmailEntity de(CorreoEnviado c) {
        SentEmailEntity e = new SentEmailEntity();
        e.id = c.id();
        e.notificationId = c.notificacionId();
        e.recipientId = c.destinatarioId();
        e.toAddress = c.para();
        e.subject = c.asunto();
        e.body = c.cuerpo();
        e.status = c.estado().name();
        e.error = c.error() == null || c.error().length() <= 500 ? c.error() : c.error().substring(0, 500);
        e.attemptedAt = c.intentadoEn();
        return e;
    }

    CorreoEnviado aDominio() {
        return new CorreoEnviado(id, notificationId, recipientId, toAddress, subject, body, EstadoCorreo.valueOf(status),
                error, attemptedAt);
    }
}
