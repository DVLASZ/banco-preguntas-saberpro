package co.unicauca.bancopreguntas.email.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI emailServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("email-service")
                .version("0.1.0")
                .description("Correos del Banco de Preguntas Saber Pro: envía (simulado) un correo por cada notificación "
                        + "creada y deja el registro de lo enviado para el administrador. Las peticiones indican "
                        + "el usuario en las cabeceras X-User-Id y X-User-Roles; en el sistema completo las propaga "
                        + "el API Gateway."));
    }
}
