package co.unicauca.bancopreguntas.lifecycle.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/** Fila de {@code question_lifecycle}: el estado actual de una pregunta. */
@Entity
@Table(name = "question_lifecycle")
class LifecycleEntity {

    @Id
    @Column(name = "question_id", length = 36)
    String preguntaId;

    @Column(name = "author_id", nullable = false, length = 100)
    String autorId;

    @Column(nullable = false, length = 30)
    String estado;

    @Column(name = "question_version", nullable = false)
    int version;

    @Column(nullable = false)
    int sequence;

    @Column(name = "updated_at", nullable = false)
    Instant actualizadoEn;

    /** Bloqueo optimista: si dos cambios llegan a la vez sobre la misma pregunta, el segundo falla en vez de pisar al primero. */
    @Version
    @Column(name = "lock_version", nullable = false)
    Long lockVersion;

    protected LifecycleEntity() {
        // requerido por JPA
    }
}
