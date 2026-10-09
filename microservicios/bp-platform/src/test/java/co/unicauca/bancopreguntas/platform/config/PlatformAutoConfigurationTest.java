package co.unicauca.bancopreguntas.platform.config;

import co.unicauca.bancopreguntas.platform.api.PlatformExceptionHandler;
import co.unicauca.bancopreguntas.platform.api.PlatformFallbackExceptionHandler;
import co.unicauca.bancopreguntas.platform.events.AmqpEventPublisher;
import co.unicauca.bancopreguntas.platform.events.IncomingEventParser;
import co.unicauca.bancopreguntas.platform.security.HeaderUserArgumentResolver;
import co.unicauca.bancopreguntas.platform.security.JwtUserArgumentResolver;
import co.unicauca.bancopreguntas.platform.security.KeycloakJwtConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Configuración automática de bp-platform")
class PlatformAutoConfigurationTest {

    private final ApplicationContextRunner eventos = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformEventsAutoConfiguration.class))
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    private final WebApplicationContextRunner web = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformWebAutoConfiguration.class, SecurityAutoConfiguration.class,
                    WebMvcAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    @DisplayName("registra el publicador, el lector de eventos, el exchange común y un reloj")
    void registraLosEventos() {
        eventos.run(contexto -> {
            assertThat(contexto).hasSingleBean(AmqpEventPublisher.class);
            assertThat(contexto).hasSingleBean(IncomingEventParser.class);
            assertThat(contexto).hasSingleBean(Clock.class);
            assertThat(contexto.getBean(TopicExchange.class).getName()).isEqualTo("bancopreguntas.events");
            assertThat(contexto.getBean(TopicExchange.class).isDurable()).isTrue();
        });
    }

    @Test
    @DisplayName("respeta el reloj que defina el servicio")
    void respetaElRelojDelServicio() {
        Clock fijo = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneOffset.UTC);

        eventos.withBean("relojDelServicio", Clock.class, () -> fijo)
                .run(contexto -> assertThat(contexto.getBean(Clock.class)).isSameAs(fijo));
    }

    /** Los resolvedores de parámetros que aportan todos los configuradores web del contexto. */
    private static List<HandlerMethodArgumentResolver> resolvedores(ApplicationContext contexto) {
        List<HandlerMethodArgumentResolver> resolvedores = new ArrayList<>();
        contexto.getBeansOfType(WebMvcConfigurer.class).values().forEach(c -> c.addArgumentResolvers(resolvedores));
        return resolvedores;
    }

    @Test
    @DisplayName("en una aplicación web registra el manejo de errores y el usuario por cabeceras")
    void registraLaWeb() {
        web.run(contexto -> {
            assertThat(contexto).hasSingleBean(PlatformExceptionHandler.class);
            assertThat(contexto).hasSingleBean(PlatformFallbackExceptionHandler.class);
            assertThat(resolvedores(contexto)).hasAtLeastOneElementOfType(HeaderUserArgumentResolver.class)
                    .noneMatch(JwtUserArgumentResolver.class::isInstance);
            assertThat(contexto).hasBean("headersSecurityFilterChain").doesNotHaveBean("jwtSecurityFilterChain");
        });
    }

    @Test
    @DisplayName("con el modo de seguridad jwt toma el usuario del token y exige autenticación")
    void modoJwt() {
        web.withBean(JwtDecoder.class, () -> mock(JwtDecoder.class))
                .withPropertyValues("bancopreguntas.security.mode=jwt").run(contexto -> {
                    assertThat(contexto).hasSingleBean(PlatformExceptionHandler.class);
                    assertThat(resolvedores(contexto)).hasAtLeastOneElementOfType(JwtUserArgumentResolver.class)
                            .noneMatch(HeaderUserArgumentResolver.class::isInstance);
                    assertThat(contexto).hasBean("jwtSecurityFilterChain").doesNotHaveBean("headersSecurityFilterChain");
                    assertThat(contexto).hasSingleBean(KeycloakJwtConverter.class);
                });
    }

    @Test
    @DisplayName("fuera de una aplicación web no registra nada de la parte web")
    void sinWeb() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlatformWebAutoConfiguration.class))
                .run(contexto -> assertThat(contexto).doesNotHaveBean(PlatformExceptionHandler.class));
    }
}
