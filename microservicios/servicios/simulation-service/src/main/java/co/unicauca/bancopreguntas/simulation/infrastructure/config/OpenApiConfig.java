package co.unicauca.bancopreguntas.simulation.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI simulationServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("simulation-service")
                .version("0.1.0")
                .description("Simulacros de evaluación del Banco de Preguntas Saber Pro (RF-21 a RF-24): generación "
                        + "aleatoria con preguntas publicadas, control del tiempo y calificación automática. Se alimenta "
                        + "del evento question.published y publica simulation.completed."));
    }
}
