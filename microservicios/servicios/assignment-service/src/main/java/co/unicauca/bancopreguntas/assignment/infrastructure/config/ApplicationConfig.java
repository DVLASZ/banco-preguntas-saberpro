package co.unicauca.bancopreguntas.assignment.infrastructure.config;

import co.unicauca.bancopreguntas.assignment.application.AssignmentApplicationService;
import co.unicauca.bancopreguntas.assignment.application.port.AssignmentRepository;
import co.unicauca.bancopreguntas.assignment.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.assignment.application.port.ProcessedEvents;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    @Bean
    AssignmentApplicationService assignmentApplicationService(AssignmentRepository repositorio,
                                                              DomainEventPublisher eventos,
                                                              ProcessedEvents procesados,
                                                              Clock reloj,
                                                              @Value("${assignment.max-reviewers:3}") int maximoDeRevisores) {
        return new AssignmentApplicationService(repositorio, eventos, procesados, reloj,
                () -> UUID.randomUUID().toString(), maximoDeRevisores);
    }
}
