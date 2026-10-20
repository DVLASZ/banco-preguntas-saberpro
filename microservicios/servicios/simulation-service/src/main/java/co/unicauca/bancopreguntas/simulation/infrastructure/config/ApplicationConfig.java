package co.unicauca.bancopreguntas.simulation.infrastructure.config;

import co.unicauca.bancopreguntas.simulation.application.SimulationApplicationService;
import co.unicauca.bancopreguntas.simulation.application.port.CatalogRepository;
import co.unicauca.bancopreguntas.simulation.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.simulation.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.simulation.application.port.SimulacroRepository;
import co.unicauca.bancopreguntas.simulation.domain.SeleccionAleatoria;
import co.unicauca.bancopreguntas.simulation.domain.SelectorDePreguntas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    /** Cómo se escogen las preguntas (patrón Strategy): hoy, al azar. */
    @Bean
    SelectorDePreguntas selectorDePreguntas() {
        return new SeleccionAleatoria(new SecureRandom());
    }

    @Bean
    SimulationApplicationService simulationApplicationService(CatalogRepository catalogo,
                                                              SimulacroRepository simulacros,
                                                              DomainEventPublisher eventos,
                                                              ProcessedEvents procesados,
                                                              SelectorDePreguntas selector,
                                                              Clock reloj,
                                                              @Value("${simulation.max-questions:50}") int maximoDePreguntas) {
        return new SimulationApplicationService(catalogo, simulacros, eventos, procesados, selector, reloj,
                () -> UUID.randomUUID().toString(), maximoDePreguntas);
    }
}
