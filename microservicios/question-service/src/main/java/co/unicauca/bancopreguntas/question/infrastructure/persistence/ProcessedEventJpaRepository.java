package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, String> {

    /** Inserta el evento si no existía; devuelve cuántas filas se insertaron (0 si ya estaba). */
    @Modifying
    @Query(value = "INSERT INTO processed_event (event_id, processed_at) VALUES (:eventId, :procesadoEn) "
            + "ON CONFLICT (event_id) DO NOTHING", nativeQuery = true)
    int insertarSiNoExiste(@Param("eventId") String eventId, @Param("procesadoEn") Instant procesadoEn);
}
