package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Consultas de Spring Data sobre la tabla {@code question}; la búsqueda con filtros está en el fragmento a medida. */
public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, String>, QuestionJpaRepositoryCustom {
}
