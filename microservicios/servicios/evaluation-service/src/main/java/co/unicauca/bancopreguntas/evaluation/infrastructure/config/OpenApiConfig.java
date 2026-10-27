package co.unicauca.bancopreguntas.evaluation.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI evaluationServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("evaluation-service")
                .version("0.1.0")
                .description("Evaluación de preguntas por pares del Banco de Preguntas Saber Pro (HU-05): recibe la "
                        + "evaluación de cada revisor, decide la ronda por unanimidad y publica "
                        + "review.evaluation-submitted y review.completed. Los revisores los asigna assignment-service. "
                        + "Las peticiones indican el usuario en las cabeceras X-User-Id y X-User-Roles; en el sistema "
                        + "completo las propaga el API Gateway."));
    }
}
