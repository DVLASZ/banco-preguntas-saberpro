package co.unicauca.bancopreguntas.question.infrastructure.config;

import co.unicauca.bancopreguntas.question.application.QuestionApplicationService;
import co.unicauca.bancopreguntas.question.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.question.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.question.application.port.QuestionRepository;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    @Bean
    QuestionValidator questionValidator() {
        return QuestionValidator.porDefecto();
    }

    @Bean
    QuestionApplicationService questionApplicationService(QuestionRepository repositorio,
                                                          DomainEventPublisher eventos,
                                                          ProcessedEvents procesados,
                                                          QuestionValidator validador,
                                                          Clock reloj) {
        return new QuestionApplicationService(repositorio, eventos, procesados, validador, reloj,
                () -> UUID.randomUUID().toString());
    }
}
