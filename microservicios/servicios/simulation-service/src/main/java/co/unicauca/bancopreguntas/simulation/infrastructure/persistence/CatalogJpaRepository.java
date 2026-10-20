package co.unicauca.bancopreguntas.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface CatalogJpaRepository extends JpaRepository<CatalogQuestionEntity, String> {

    List<CatalogQuestionEntity> findByAvailableTrue();

    @Query("select c.competency, count(c) from CatalogQuestionEntity c where c.available = true group by c.competency")
    List<Object[]> contarPorCompetencia();

    @Query("select c.difficulty, count(c) from CatalogQuestionEntity c where c.available = true group by c.difficulty")
    List<Object[]> contarPorDificultad();
}
