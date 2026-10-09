package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Búsqueda de preguntas con filtros opcionales, armada a medida de los filtros presentes. */
public interface QuestionJpaRepositoryCustom {

    /**
     * Busca preguntas, más recientes primero. Un parámetro nulo no filtra. El texto (ya con sus comodines de LIKE)
     * se compara sin distinguir mayúsculas ni tildes (extensión {@code unaccent}) contra nombre, pregunta directa,
     * tema y subtema (RF-07).
     */
    Page<QuestionEntity> buscar(String autor, String estado, String competencia, String texto, Pageable pageable);
}
