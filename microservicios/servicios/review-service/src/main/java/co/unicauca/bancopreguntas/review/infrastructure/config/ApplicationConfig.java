package co.unicauca.bancopreguntas.review.infrastructure.config;

import co.unicauca.bancopreguntas.review.application.ReviewApplicationService;
import co.unicauca.bancopreguntas.review.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.review.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.review.application.port.ReviewCaseRepository;
import co.unicauca.bancopreguntas.review.domain.PoliticaDeDecision;
import co.unicauca.bancopreguntas.review.domain.Unanimidad;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    /** Regla con la que se decide una ronda (patrón Strategy): hoy, unanimidad. */
    @Bean
    PoliticaDeDecision politicaDeDecision() {
        return new Unanimidad();
    }

    @Bean
    ReviewApplicationService reviewApplicationService(ReviewCaseRepository repositorio,
                                                      DomainEventPublisher eventos,
                                                      ProcessedEvents procesados,
                                                      PoliticaDeDecision politica,
                                                      Clock reloj,
                                                      @Value("${review.max-reviewers:3}") int maximoDeRevisores) {
        return new ReviewApplicationService(repositorio, eventos, procesados, politica, reloj,
                () -> UUID.randomUUID().toString(), maximoDeRevisores);
    }
}
