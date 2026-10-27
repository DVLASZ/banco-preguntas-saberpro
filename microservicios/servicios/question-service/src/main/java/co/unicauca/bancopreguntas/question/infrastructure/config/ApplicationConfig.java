package co.unicauca.bancopreguntas.question.infrastructure.config;

import co.unicauca.bancopreguntas.question.application.QuestionApplicationService;
import co.unicauca.bancopreguntas.question.application.port.CicloDeVidaDePreguntas;
import co.unicauca.bancopreguntas.question.application.port.QuestionRepository;
import co.unicauca.bancopreguntas.question.application.port.ValidadorDeContenido;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    @Bean
    QuestionApplicationService questionApplicationService(QuestionRepository repositorio,
                                                          ValidadorDeContenido validador,
                                                          CicloDeVidaDePreguntas ciclo,
                                                          Clock reloj) {
        return new QuestionApplicationService(repositorio, validador, ciclo, reloj, () -> UUID.randomUUID().toString());
    }
}
