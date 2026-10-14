package co.unicauca.bancopreguntas.notification.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI notificationServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("notification-service")
                .version("0.1.0")
                .description("Notificaciones del Banco de Preguntas Saber Pro: bandeja de cada usuario y correos "
                        + "simulados. Se alimenta de los eventos de preguntas y de revisión. Las peticiones indican "
                        + "el usuario en las cabeceras X-User-Id y X-User-Roles; en el sistema completo las propaga "
                        + "el API Gateway."));
    }
}
