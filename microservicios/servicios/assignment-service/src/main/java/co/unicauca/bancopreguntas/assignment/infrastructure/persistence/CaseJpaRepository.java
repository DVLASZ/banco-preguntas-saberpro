package co.unicauca.bancopreguntas.assignment.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CaseJpaRepository extends JpaRepository<CaseEntity, String> {

    Optional<CaseEntity> findByQuestionId(String questionId);

    /** Bloquea la fila del caso hasta el fin de la transacción, para serializar cambios simultáneos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CaseEntity c where c.questionId = :questionId")
    Optional<CaseEntity> findByQuestionIdForUpdate(@Param("questionId") String questionId);

    @Query(value = "select new co.unicauca.bancopreguntas.assignment.infrastructure.persistence"
            + ".PendingRow(c.questionId, c.title, c.competency, c.authorId, r.number, r.startedAt) "
            + "from RoundEntity r join r.assignmentCase c where r.state = 'PENDIENTE_ASIGNACION' "
            + "order by r.startedAt asc, c.questionId asc",
            countQuery = "select count(r) from RoundEntity r where r.state = 'PENDIENTE_ASIGNACION'")
    Page<PendingRow> pendingAssignment(Pageable pageable);

    /**
     * La bandeja de un revisor. {@code filtro} es PENDIENTES, EVALUADAS o TODAS.
     */
    @Query(value = "select new co.unicauca.bancopreguntas.assignment.infrastructure.persistence"
            + ".InboxRow(c.questionId, c.title, c.competency, c.authorId, r.number, "
            + "a.assignedAt, r.state, a.decision) "
            + "from AssignmentEntity a join a.round r join r.assignmentCase c "
            + "where a.reviewerId = :reviewerId and ("
            + " :filtro = 'TODAS'"
            + " or (:filtro = 'PENDIENTES' and a.decision is null and r.state in ('EN_CURSO', 'RECHAZADA'))"
            + " or (:filtro = 'EVALUADAS' and a.decision is not null)) "
            + "order by a.assignedAt desc, c.questionId asc",
            countQuery = "select count(a) from AssignmentEntity a join a.round r "
                    + "where a.reviewerId = :reviewerId and ("
                    + " :filtro = 'TODAS'"
                    + " or (:filtro = 'PENDIENTES' and a.decision is null and r.state in ('EN_CURSO', 'RECHAZADA'))"
                    + " or (:filtro = 'EVALUADAS' and a.decision is not null))")
    Page<InboxRow> inbox(@Param("reviewerId") String reviewerId, @Param("filtro") String filtro, Pageable pageable);
}
