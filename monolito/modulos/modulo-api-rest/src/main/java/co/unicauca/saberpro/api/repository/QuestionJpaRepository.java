package co.unicauca.saberpro.api.repository;

import co.unicauca.saberpro.api.model.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionJpaRepository extends JpaRepository<QuestionEntity, String> {

    @Query("select q.id from QuestionEntity q")
    List<String> findAllIds();
}
