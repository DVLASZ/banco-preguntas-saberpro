package co.unicauca.bancopreguntas.platform.config;

import co.unicauca.bancopreguntas.platform.client.IdentityPropagationInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Parte de clientes REST de la configuración transversal: todo {@code RestClient.Builder} que se inyecte en un servicio
 * web ya propaga la identidad de quien hizo la petición (ver {@link IdentityPropagationInterceptor}).
 */
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PlatformClientAutoConfiguration {

    @Bean
    RestClientCustomizer identityPropagationCustomizer() {
        return builder -> builder.requestInterceptor(new IdentityPropagationInterceptor());
    }
}
