package co.unicauca.bancopreguntas.history.infrastructure.persistence;

import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de {@code history_entry}. Es un detalle de persistencia: el dominio usa {@code EntradaDeHistorial}. */
@Entity
@Table(name = "history_entry")
public class HistoryEntryEntity {

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

    protected HistoryEntryEntity() {
        // requerido por JPA
    }

    static HistoryEntryEntity de(EntradaDeHistorial e) {
        HistoryEntryEntity entidad = new HistoryEntryEntity();
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

    EntradaDeHistorial aDominio() {
        return new EntradaDeHistorial(id, eventId, questionId, actorId, TipoEntrada.valueOf(type), summary, details,
                occurredAt, recordedAt);
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getQuestionId() { return questionId; }
    public String getActorId() { return actorId; }
    public String getType() { return type; }
    public Instant getOccurredAt() { return occurredAt; }
}
