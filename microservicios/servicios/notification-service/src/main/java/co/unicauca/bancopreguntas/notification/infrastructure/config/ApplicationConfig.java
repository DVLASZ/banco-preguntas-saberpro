package co.unicauca.bancopreguntas.notification.infrastructure.config;

import co.unicauca.bancopreguntas.notification.application.NotificationApplicationService;
import co.unicauca.bancopreguntas.notification.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.notification.application.port.NotificationRepository;
import co.unicauca.bancopreguntas.notification.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.notification.application.port.QuestionInfoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    @Bean
    NotificationApplicationService notificationApplicationService(NotificationRepository notificaciones,
                                                                  QuestionInfoRepository preguntas,
                                                                  DomainEventPublisher eventos,
                                                                  ProcessedEvents procesados,
                                                                  Clock reloj) {
        return new NotificationApplicationService(notificaciones, preguntas, eventos, procesados, reloj,
                () -> UUID.randomUUID().toString());
    }
}
