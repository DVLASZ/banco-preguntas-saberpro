package co.unicauca.bancopreguntas.validation.infrastructure.config;

import co.unicauca.bancopreguntas.validation.application.ValidationApplicationService;
import co.unicauca.bancopreguntas.validation.domain.validation.QuestionValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla la capa de aplicación con el conjunto de reglas; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    @Bean
    QuestionValidator questionValidator() {
        return QuestionValidator.porDefecto();
    }

    @Bean
    ValidationApplicationService validationApplicationService(QuestionValidator validador) {
        return new ValidationApplicationService(validador);
    }
}
