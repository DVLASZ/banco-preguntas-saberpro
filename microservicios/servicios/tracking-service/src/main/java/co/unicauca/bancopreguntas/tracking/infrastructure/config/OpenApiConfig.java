package co.unicauca.bancopreguntas.tracking.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI trackingServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("tracking-service")
                .version("0.1.0")
                .description("Seguimiento académico del Banco de Preguntas Saber Pro (RF-25 a RF-28): historial de "
                        + "simulacros por estudiante, estadísticas individuales, fortalezas y debilidades por "
                        + "competencia y reportes para docentes y coordinadores. Se alimenta del evento "
                        + "simulation.completed."));
    }
}
