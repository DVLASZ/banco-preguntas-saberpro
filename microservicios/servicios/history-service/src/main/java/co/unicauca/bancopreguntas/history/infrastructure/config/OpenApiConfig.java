package co.unicauca.bancopreguntas.history.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales de la documentación Swagger/OpenAPI de este servicio. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI historyServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("history-service")
                .version("0.1.0")
                .description("Historial de las preguntas del Banco de Preguntas Saber Pro. Se alimenta de "
                        + "los eventos de los demás servicios. Cada rol ve una parte distinta: el autor, el rastro de "
                        + "sus preguntas; el revisor, las que revisa o revisó; el administrador, todas. "
                        + "Las peticiones indican el usuario en las cabeceras X-User-Id y X-User-Roles; en el sistema "
                        + "completo las propaga el API Gateway."));
    }
}
