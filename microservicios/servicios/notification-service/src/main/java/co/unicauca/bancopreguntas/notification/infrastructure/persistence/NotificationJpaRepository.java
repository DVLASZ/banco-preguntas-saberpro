package co.unicauca.bancopreguntas.notification.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {

    Page<NotificationEntity> findByRecipientIdOrderByCreatedAtDescIdAsc(String recipientId, Pageable pageable);

    Page<NotificationEntity> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDescIdAsc(String recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(String recipientId);

    Page<NotificationEntity> findByEmailSentAtIsNotNullOrderByEmailSentAtDescIdAsc(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update NotificationEntity n set n.readAt = :ahora where n.recipientId = :usuario and n.readAt is null")
    int marcarTodasComoLeidas(@Param("usuario") String usuario, @Param("ahora") Instant ahora);
}
