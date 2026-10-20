package co.unicauca.bancopreguntas.history.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionReviewerJpaRepository extends JpaRepository<QuestionReviewerEntity, Long> {

    boolean existsByQuestionIdAndReviewerId(String questionId, String reviewerId);

    /** Agrega al revisor si no estaba; devuelve cuántas filas se insertaron. */
    @Modifying
    @Query(value = "INSERT INTO question_reviewer (question_id, reviewer_id) VALUES (:preguntaId, :revisorId) "
            + "ON CONFLICT (question_id, reviewer_id) DO NOTHING", nativeQuery = true)
    int agregar(@Param("preguntaId") String preguntaId, @Param("revisorId") String revisorId);
}
