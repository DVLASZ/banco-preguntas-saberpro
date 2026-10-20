package co.unicauca.bancopreguntas.tracking.infrastructure.config;

import co.unicauca.bancopreguntas.tracking.application.TrackingApplicationService;
import co.unicauca.bancopreguntas.tracking.application.port.HistorialRepository;
import co.unicauca.bancopreguntas.tracking.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.tracking.domain.Umbrales;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    @Bean
    Umbrales umbrales(@Value("${tracking.strength-threshold:70}") double fortaleza,
                      @Value("${tracking.weakness-threshold:50}") double debilidad,
                      @Value("${tracking.min-answered:5}") int minimoDePreguntas) {
        return new Umbrales(fortaleza, debilidad, minimoDePreguntas);
    }

    @Bean
    TrackingApplicationService trackingApplicationService(HistorialRepository historial, ProcessedEvents procesados,
                                                          Umbrales umbrales, Clock reloj) {
        return new TrackingApplicationService(historial, procesados, umbrales, reloj);
    }
}
