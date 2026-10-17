package co.unicauca.bancopreguntas.user.infrastructure.config;

import co.unicauca.bancopreguntas.user.application.UserApplicationService;
import co.unicauca.bancopreguntas.user.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.user.application.port.IdentityProvider;
import co.unicauca.bancopreguntas.user.application.port.SessionCursor;
import co.unicauca.bancopreguntas.user.application.port.UserRepository;
import co.unicauca.bancopreguntas.user.infrastructure.keycloak.KeycloakIdentityProvider;
import co.unicauca.bancopreguntas.user.infrastructure.keycloak.KeycloakProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/** Ensambla la capa de aplicación con sus adaptadores; el dominio no conoce a Spring. */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class ApplicationConfig {

    @Bean
    @ConditionalOnMissingBean(IdentityProvider.class)
    IdentityProvider identityProvider(KeycloakProperties propiedades, Clock reloj) {
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        return new KeycloakIdentityProvider(RestClient.builder().requestFactory(fabrica).build(), propiedades, reloj);
    }

    @Bean
    UserApplicationService userApplicationService(IdentityProvider identidad, UserRepository usuarios,
                                                  SessionCursor cursor, DomainEventPublisher eventos, Clock reloj) {
        return new UserApplicationService(identidad, usuarios, cursor, eventos, reloj);
    }
}
