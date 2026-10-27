package co.unicauca.bancopreguntas.lifecycle.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lifecycleServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("lifecycle-service")
                .version("0.1.0")
                .description("Ciclo de vida de las preguntas del Banco de Preguntas Saber Pro (RF-14 y RF-15): estados, "
                        + "transiciones, historial y eventos question.submitted, question.state-changed, "
                        + "question.reopened y question.published. Reacciona a los eventos de revisión."));
    }
}
