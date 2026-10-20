package co.unicauca.bancopreguntas.simulation.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

interface SimulationJpaRepository extends JpaRepository<SimulationEntity, String> {

    Page<SimulationEntity> findByStudentIdOrderByStartedAtDesc(String studentId, Pageable pageable);

    List<SimulationEntity> findByStatusAndDueAtBefore(String status, Instant limite, Pageable pageable);
}
