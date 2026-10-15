package co.unicauca.bancopreguntas.review.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI reviewServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("review-service")
                .version("0.1.0")
                .description("Revisión de preguntas por pares del Banco de Preguntas Saber Pro: asignación de "
                        + "revisores (HU-04) y evaluación (HU-05). Las peticiones indican el usuario en las "
                        + "cabeceras X-User-Id y X-User-Roles; en el sistema completo las propaga el API Gateway."));
    }
}
