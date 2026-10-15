package co.unicauca.bancopreguntas.review.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = "select new co.unicauca.bancopreguntas.review.infrastructure.persistence"
            + ".PendingRow(c.questionId, c.title, c.competency, c.authorId, r.number, r.startedAt) "
            + "from ReviewRoundEntity r join r.reviewCase c where r.state = 'PENDIENTE_ASIGNACION' "
            + "order by r.startedAt asc, c.questionId asc",
            countQuery = "select count(r) from ReviewRoundEntity r where r.state = 'PENDIENTE_ASIGNACION'")
    Page<PendingRow> pendingAssignment(Pageable pageable);

    /**
     * La bandeja de un revisor. {@code filtro} es PENDIENTES, EVALUADAS o TODAS.
     */
    @Query(value = "select new co.unicauca.bancopreguntas.review.infrastructure.persistence"
            + ".InboxRow(c.questionId, c.title, c.competency, c.authorId, r.number, "
            + "a.assignedAt, r.state, e.decision) "
            + "from AssignmentEntity a join a.round r join r.reviewCase c "
            + "left join EvaluationEntity e on e.round = r and e.reviewerId = a.reviewerId "
            + "where a.reviewerId = :reviewerId and ("
            + " :filtro = 'TODAS'"
            + " or (:filtro = 'PENDIENTES' and e.id is null and r.state in ('EN_CURSO', 'RECHAZADA'))"
            + " or (:filtro = 'EVALUADAS' and e.id is not null)) "
            + "order by a.assignedAt desc, c.questionId asc",
            countQuery = "select count(a) from AssignmentEntity a join a.round r "
                    + "left join EvaluationEntity e on e.round = r and e.reviewerId = a.reviewerId "
                    + "where a.reviewerId = :reviewerId and ("
                    + " :filtro = 'TODAS'"
                    + " or (:filtro = 'PENDIENTES' and e.id is null and r.state in ('EN_CURSO', 'RECHAZADA'))"
                    + " or (:filtro = 'EVALUADAS' and e.id is not null))")
    Page<InboxRow> inbox(@Param("reviewerId") String reviewerId, @Param("filtro") String filtro, Pageable pageable);
}
