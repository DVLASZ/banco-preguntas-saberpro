package co.unicauca.bancopreguntas.email.infrastructure.config;

import co.unicauca.bancopreguntas.email.application.EmailApplicationService;
import co.unicauca.bancopreguntas.email.application.port.EmailSender;
import co.unicauca.bancopreguntas.email.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.email.application.port.SentEmailRepository;
import co.unicauca.bancopreguntas.email.infrastructure.messaging.EmailConRegistro;
import co.unicauca.bancopreguntas.email.infrastructure.messaging.EmailConReintentos;
import co.unicauca.bancopreguntas.email.infrastructure.messaging.SimulatedEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
public class ApplicationConfig {

    /**
     * El envío de correo, armado con decoradores (patrón Decorator): el registro envuelve a los reintentos y estos al
     * envío simulado. Conectar un servidor real es reemplazar solo el envío del centro.
     */
    @Bean
    EmailSender emailSender(@Value("${email.attempts:3}") int intentos) {
        return new EmailConRegistro(new EmailConReintentos(new SimulatedEmailSender(), intentos));
    }

    @Bean
    EmailApplicationService emailApplicationService(EmailSender correo, SentEmailRepository enviados,
                                                    ProcessedEvents procesados, Clock reloj,
                                                    @Value("${email.domain}") String dominio) {
        return new EmailApplicationService(correo, enviados, procesados, reloj, () -> UUID.randomUUID().toString(),
                usuario -> usuario + "@" + dominio);
    }
}
