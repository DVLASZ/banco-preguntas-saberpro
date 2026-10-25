package co.unicauca.bancopreguntas.email.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consultas de Spring Data sobre {@code sent_email}. */
public interface SentEmailJpaRepository extends JpaRepository<SentEmailEntity, String> {

    Page<SentEmailEntity> findByOrderByAttemptedAtDescIdAsc(Pageable pageable);

    Page<SentEmailEntity> findByStatusOrderByAttemptedAtDescIdAsc(String status, Pageable pageable);
}
