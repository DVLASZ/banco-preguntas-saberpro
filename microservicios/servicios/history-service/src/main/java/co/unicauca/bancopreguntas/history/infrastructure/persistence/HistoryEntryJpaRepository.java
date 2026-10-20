package co.unicauca.bancopreguntas.history.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface HistoryEntryJpaRepository
        extends JpaRepository<HistoryEntryEntity, String>, JpaSpecificationExecutor<HistoryEntryEntity> {

    List<HistoryEntryEntity> findByQuestionIdOrderByOccurredAtAscIdAsc(String questionId);

    List<HistoryEntryEntity> findByQuestionIdAndTypeInOrderByOccurredAtAscIdAsc(String questionId, Collection<String> tipos);

    /**
     * Lo que ve un usuario según sus roles: el administrador, todo; el autor, los tipos que le corresponden de sus
     * preguntas; el revisor, todo lo de las preguntas que revisa o revisó.
     */
    @Query(value = "select e from HistoryEntryEntity e where :admin = true or (e.questionId is not null and ("
            + "(:autor = true and e.type in :tiposAutor and e.questionId in "
            + "(select q.questionId from QuestionInfoEntity q where q.authorId = :usuario)) "
            + "or (:revisor = true and e.questionId in "
            + "(select r.questionId from QuestionReviewerEntity r where r.reviewerId = :usuario)))) "
            + "order by e.occurredAt desc, e.id asc",
            countQuery = "select count(e) from HistoryEntryEntity e where :admin = true or (e.questionId is not null and ("
                    + "(:autor = true and e.type in :tiposAutor and e.questionId in "
                    + "(select q.questionId from QuestionInfoEntity q where q.authorId = :usuario)) "
                    + "or (:revisor = true and e.questionId in "
                    + "(select r.questionId from QuestionReviewerEntity r where r.reviewerId = :usuario))))")
    Page<HistoryEntryEntity> visiblesPara(@Param("admin") boolean admin, @Param("autor") boolean autor,
                                          @Param("revisor") boolean revisor, @Param("usuario") String usuario,
                                          @Param("tiposAutor") Collection<String> tiposAutor, Pageable pageable);
}
