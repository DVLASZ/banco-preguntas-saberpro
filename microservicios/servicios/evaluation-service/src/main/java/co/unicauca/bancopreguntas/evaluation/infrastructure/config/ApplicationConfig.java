package co.unicauca.bancopreguntas.evaluation.infrastructure.config;

import co.unicauca.bancopreguntas.evaluation.application.EvaluationApplicationService;
import co.unicauca.bancopreguntas.evaluation.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.evaluation.application.port.EvaluationRepository;
import co.unicauca.bancopreguntas.evaluation.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.evaluation.domain.PoliticaDeDecision;
import co.unicauca.bancopreguntas.evaluation.domain.Unanimidad;
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
    EvaluationApplicationService evaluationApplicationService(EvaluationRepository repositorio,
                                                              DomainEventPublisher eventos,
                                                              ProcessedEvents procesados,
                                                              PoliticaDeDecision politica,
                                                              Clock reloj) {
        return new EvaluationApplicationService(repositorio, eventos, procesados, politica, reloj,
                () -> UUID.randomUUID().toString());
    }
}
