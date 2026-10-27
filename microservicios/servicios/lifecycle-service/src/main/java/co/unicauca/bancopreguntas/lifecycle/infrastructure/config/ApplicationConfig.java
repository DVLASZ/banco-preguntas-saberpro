package co.unicauca.bancopreguntas.lifecycle.infrastructure.config;

import co.unicauca.bancopreguntas.lifecycle.application.LifecycleApplicationService;
import co.unicauca.bancopreguntas.lifecycle.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.lifecycle.application.port.LifecycleRepository;
import co.unicauca.bancopreguntas.lifecycle.application.port.ProcessedEvents;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    @Bean
    LifecycleApplicationService lifecycleApplicationService(LifecycleRepository repositorio,
                                                            DomainEventPublisher eventos,
                                                            ProcessedEvents procesados,
                                                            Clock reloj) {
        return new LifecycleApplicationService(repositorio, eventos, procesados, reloj);
    }
}
