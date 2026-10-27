package co.unicauca.bancopreguntas.assignment.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI assignmentServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("assignment-service")
                .version("0.1.0")
                .description("Asignación de revisores del Banco de Preguntas Saber Pro (HU-04) y bandeja del revisor: "
                        + "abre una ronda por cada envío a revisión, deja que el administrador asigne revisores y "
                        + "publica review.reviewers-assigned. Las peticiones indican el usuario en las cabeceras "
                        + "X-User-Id y X-User-Roles; en el sistema completo las propaga el API Gateway."));
    }
}
