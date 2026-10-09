package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StateHistoryJpaRepository extends JpaRepository<StateHistoryEntity, Long> {

    List<StateHistoryEntity> findByQuestionIdOrderByFechaAscIdAsc(String questionId);
}
