package co.unicauca.bancopreguntas.user.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI userServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("user-service")
                .version("0.1.0")
                .description("Directorio de usuarios del Banco de Preguntas Saber Pro, sincronizado con Keycloak (la "
                        + "fuente de verdad de usuarios, roles y contraseñas), y auditoría de inicios de sesión. El "
                        + "administrador lo usa para elegir revisores."));
    }
}
