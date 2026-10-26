package co.unicauca.bancopreguntas.audit.infrastructure.config;

import co.unicauca.bancopreguntas.audit.application.AuditApplicationService;
import co.unicauca.bancopreguntas.audit.application.port.AuditRepository;
import co.unicauca.bancopreguntas.audit.application.port.ProcessedEvents;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    @Bean
    AuditApplicationService auditApplicationService(AuditRepository auditoria, ProcessedEvents procesados, Clock reloj) {
        return new AuditApplicationService(auditoria, procesados, reloj, () -> UUID.randomUUID().toString());
    }
}
