package co.unicauca.bancopreguntas.user.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** La única fila de {@code session_cursor}. */
@Entity
@Table(name = "session_cursor")
public class SessionCursorEntity {

    public static final int ID = 1;

    @Id
    private Integer id;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    protected SessionCursorEntity() {
        // requerido por JPA
    }

    public Instant getLastEventAt() { return lastEventAt; }
    public void setLastEventAt(Instant lastEventAt) { this.lastEventAt = lastEventAt; }
}
