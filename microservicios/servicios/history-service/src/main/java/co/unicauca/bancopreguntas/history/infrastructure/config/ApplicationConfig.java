package co.unicauca.bancopreguntas.history.infrastructure.config;

import co.unicauca.bancopreguntas.history.application.HistoryApplicationService;
import co.unicauca.bancopreguntas.history.application.port.HistoryRepository;
import co.unicauca.bancopreguntas.history.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.history.application.port.QuestionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    @Bean
    HistoryApplicationService historyApplicationService(HistoryRepository historial, QuestionRegistry preguntas,
                                                        ProcessedEvents procesados, Clock reloj) {
        return new HistoryApplicationService(historial, preguntas, procesados, reloj, () -> UUID.randomUUID().toString());
    }
}
