package co.unicauca.bancopreguntas.notification.infrastructure.config;

import co.unicauca.bancopreguntas.notification.application.NotificationApplicationService;
import co.unicauca.bancopreguntas.notification.application.port.EmailSender;
import co.unicauca.bancopreguntas.notification.application.port.NotificationRepository;
import co.unicauca.bancopreguntas.notification.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.notification.application.port.QuestionInfoRepository;
import org.springframework.beans.factory.annotation.Value;
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
                                                                  EmailSender correo,
                                                                  ProcessedEvents procesados,
                                                                  Clock reloj,
                                                                  @Value("${notification.email-domain}") String dominio) {
        return new NotificationApplicationService(notificaciones, preguntas, correo, procesados, reloj,
                () -> UUID.randomUUID().toString(), usuario -> usuario + "@" + dominio);
    }
}
