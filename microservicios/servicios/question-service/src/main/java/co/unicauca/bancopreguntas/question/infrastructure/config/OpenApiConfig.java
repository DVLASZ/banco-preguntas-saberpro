package co.unicauca.bancopreguntas.question.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI questionServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("question-service")
                .version("0.1.0")
                .description("Contenido de las preguntas del Banco de Preguntas Saber Pro (HU-01, HU-02 y HU-03): crear, "
                        + "modificar, consultar y filtrar. La validación estructural la hace validation-service y los estados "
                        + "los lleva lifecycle-service; aquí queda una copia del estado. Las peticiones indican el usuario en la "
                        + "cabecera X-User-Id; en el sistema completo la propaga el API Gateway."));
    }
}
