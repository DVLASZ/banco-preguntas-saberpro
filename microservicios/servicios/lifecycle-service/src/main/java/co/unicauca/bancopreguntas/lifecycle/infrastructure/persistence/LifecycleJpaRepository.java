package co.unicauca.bancopreguntas.lifecycle.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface LifecycleJpaRepository extends JpaRepository<LifecycleEntity, String> {
}
