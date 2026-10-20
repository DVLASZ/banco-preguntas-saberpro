package co.unicauca.bancopreguntas.tracking.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ResultEntryJpaRepository extends JpaRepository<ResultEntryEntity, String> {

    Page<ResultEntryEntity> findByStudentIdOrderByFinishedAtDesc(String studentId, Pageable pageable);

    List<ResultEntryEntity> findByStudentId(String studentId);
}
