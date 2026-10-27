package co.unicauca.bancopreguntas.evaluation.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewCaseJpaRepository extends JpaRepository<ReviewCaseEntity, String> {

    Optional<ReviewCaseEntity> findByQuestionId(String questionId);

    /** Bloquea la fila del caso hasta el fin de la transacción, para serializar evaluaciones simultáneas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ReviewCaseEntity c where c.questionId = :questionId")
    Optional<ReviewCaseEntity> findByQuestionIdForUpdate(@Param("questionId") String questionId);
}
