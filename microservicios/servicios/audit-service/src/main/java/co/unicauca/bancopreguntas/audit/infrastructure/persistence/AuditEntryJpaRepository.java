package co.unicauca.bancopreguntas.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Consultas de Spring Data sobre {@code audit_entry}; los filtros se arman con especificaciones. */
public interface AuditEntryJpaRepository extends JpaRepository<AuditEntryEntity, String>, JpaSpecificationExecutor<AuditEntryEntity> {
}
