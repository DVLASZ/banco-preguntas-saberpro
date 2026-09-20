package co.unicauca.saberpro.api.config;

import co.unicauca.saberpro.preguntas.domain.QuestionRepository;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El dominio no usa anotaciones de Spring: aqui se registra su servicio como bean. */
@Configuration
public class QuestionServiceConfig {

    @Bean
    public QuestionService questionService(QuestionRepository questionRepository) {
        return new QuestionService(questionRepository);
    }
}
