package co.unicauca.bancopreguntas.audit.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI auditServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("audit-service")
                .version("0.1.0")
                .description("Auditoría del Banco de Preguntas Saber Pro: todo lo que ocurre con las preguntas y su "
                        + "revisión y los inicios de sesión, a partir de los eventos de los demás servicios. Solo la "
                        + "consulta el administrador. Las peticiones indican el usuario en las cabeceras X-User-Id y X-User-Roles; en el sistema "
                        + "completo las propaga el API Gateway."));
    }
}
