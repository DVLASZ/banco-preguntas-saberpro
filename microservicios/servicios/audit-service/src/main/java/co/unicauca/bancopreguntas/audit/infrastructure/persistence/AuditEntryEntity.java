package co.unicauca.bancopreguntas.audit.infrastructure.persistence;

import co.unicauca.bancopreguntas.audit.domain.EntradaDeAuditoria;
import co.unicauca.bancopreguntas.audit.domain.TipoEntrada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de {@code audit_entry}. Es un detalle de persistencia: el dominio usa {@code EntradaDeAuditoria}. */
@Entity
@Table(name = "audit_entry")
public class AuditEntryEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "question_id")
    private String questionId;

    @Column(name = "actor_id")
    private String actorId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected AuditEntryEntity() {
        // requerido por JPA
    }

    static AuditEntryEntity de(EntradaDeAuditoria e) {
        AuditEntryEntity entidad = new AuditEntryEntity();
        entidad.id = e.id();
        entidad.eventId = e.eventId();
        entidad.questionId = e.preguntaId();
        entidad.actorId = e.actorId();
        entidad.type = e.tipo().name();
        entidad.summary = e.resumen();
        entidad.details = e.detalles();
        entidad.occurredAt = e.ocurridaEn();
        entidad.recordedAt = e.registradaEn();
        return entidad;
    }

    EntradaDeAuditoria aDominio() {
        return new EntradaDeAuditoria(id, eventId, questionId, actorId, TipoEntrada.valueOf(type), summary, details,
                occurredAt, recordedAt);
    }
}
