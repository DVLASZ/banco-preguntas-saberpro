package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de {@code processed_event}: un evento que ya se procesó. */
@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {
        // requerido por JPA
    }

    public String getEventId() { return eventId; }
    public Instant getProcessedAt() { return processedAt; }
}
