package co.unicauca.bancopreguntas.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SessionCursorJpaRepository extends JpaRepository<SessionCursorEntity, Integer> {

    /** Toma la fila bloqueándola; si otra transacción ya la tiene, devuelve vacío en lugar de esperar. */
    @Query(value = "SELECT * FROM session_cursor WHERE id = 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<SessionCursorEntity> tomarSinEsperar();
}
