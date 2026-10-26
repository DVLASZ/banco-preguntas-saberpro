package co.unicauca.bancopreguntas.validation.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI validationServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("validation-service")
                .version("0.1.0")
                .description("Validación estructural de preguntas del Banco de Preguntas Saber Pro (HU-03, RF-08 a RF-13): "
                        + "revisa el contenido contra las reglas de calidad y reporta todos los campos que incumplen. "
                        + "No guarda datos ni publica eventos."));
    }
}
