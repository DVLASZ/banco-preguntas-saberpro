package co.unicauca.bancopreguntas.history.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionInfoJpaRepository extends JpaRepository<QuestionInfoEntity, String> {
}
