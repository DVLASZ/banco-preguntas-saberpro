package co.unicauca.bancopreguntas.platform.config;

import co.unicauca.bancopreguntas.platform.api.ApiError;
import co.unicauca.bancopreguntas.platform.api.PlatformExceptionHandler;
import co.unicauca.bancopreguntas.platform.api.PlatformFallbackExceptionHandler;
import co.unicauca.bancopreguntas.platform.security.HeaderUserArgumentResolver;
import co.unicauca.bancopreguntas.platform.security.JwtUserArgumentResolver;
import co.unicauca.bancopreguntas.platform.security.KeycloakJwtConverter;
import co.unicauca.bancopreguntas.platform.security.UserHeadersOpenApiCustomizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.List;

/**
 * Parte web de la configuración transversal: manejo de errores, usuario autenticado, seguridad y Swagger.
 * Basta agregar la dependencia {@code bp-platform} a un servicio web para tenerla.
 *
 * <p>La seguridad tiene dos modos, según {@code bancopreguntas.security.mode}:
 * <ul>
 *   <li>{@code headers} (por defecto): la identidad llega en las cabeceras {@code X-User-Id} y
 *       {@code X-User-Roles}; sirve para pruebas y desarrollo sin Keycloak.</li>
 *   <li>{@code jwt}: cada servicio valida el token JWT que emite Keycloak (autorización redundante con el
 *       gateway) y toma de él el usuario y los roles.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PlatformWebAutoConfiguration {

    @Bean
    PlatformExceptionHandler platformExceptionHandler() {
        return new PlatformExceptionHandler();
    }

    @Bean
    PlatformFallbackExceptionHandler platformFallbackExceptionHandler() {
        return new PlatformFallbackExceptionHandler();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "bancopreguntas.security.mode", havingValue = "headers", matchIfMissing = true)
    static class HeadersSecurity {

        @Bean
        WebMvcConfigurer headerUserConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                    resolvers.add(new HeaderUserArgumentResolver());
                }
            };
        }

        /** Sin tokens no hay nada que autenticar en Spring Security: la identidad la comprueban los controladores. */
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 10)
        SecurityFilterChain headersSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }

        /** Evita que Spring Boot genere (y escriba en el registro) una contraseña que nadie usa. */
        @Bean
        UserDetailsService noUsersDetailsService() {
            return new InMemoryUserDetailsManager();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "bancopreguntas.security.mode", havingValue = "jwt")
    static class JwtSecurity {

        @Bean
        WebMvcConfigurer jwtUserConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                    resolvers.add(new JwtUserArgumentResolver());
                }
            };
        }

        @Bean
        KeycloakJwtConverter keycloakJwtConverter(
                @Value("${bancopreguntas.security.user-id-claim:preferred_username}") String claimDelUsuario) {
            return new KeycloakJwtConverter(claimDelUsuario);
        }

        /**
         * Todo exige un token válido salvo la salud del servicio y la documentación. Los roles de cada operación
         * los exige el controlador.
         */
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 10)
        SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http, KeycloakJwtConverter conversor,
                                                   ObjectMapper json) throws Exception {
            AuthenticationEntryPoint sinCredenciales = (peticion, respuesta, e) -> escribir(respuesta, json,
                    HttpStatus.UNAUTHORIZED, ApiError.de("NO_AUTENTICADO", "Falta un token de acceso válido"));
            AccessDeniedHandler sinPermiso = (peticion, respuesta, e) -> escribir(respuesta, json,
                    HttpStatus.FORBIDDEN, ApiError.de("ACCESO_DENEGADO", "No tienes permiso para esta operación"));
            http.csrf(AbstractHttpConfigurer::disable)
                    .cors(Customizer.withDefaults())
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(a -> a
                            .requestMatchers("/actuator/health/**", "/actuator/info", "/v3/api-docs/**",
                                    "/swagger-ui/**", "/swagger-ui.html").permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(o -> o
                            .jwt(j -> j.jwtAuthenticationConverter(conversor))
                            .authenticationEntryPoint(sinCredenciales)
                            .accessDeniedHandler(sinPermiso))
                    .exceptionHandling(e -> e.authenticationEntryPoint(sinCredenciales).accessDeniedHandler(sinPermiso));
            return http.build();
        }

        private static void escribir(HttpServletResponse respuesta, ObjectMapper json, HttpStatus estado, ApiError error)
                throws IOException {
            respuesta.setStatus(estado.value());
            respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
            respuesta.setCharacterEncoding("UTF-8");
            json.writeValue(respuesta.getOutputStream(), error);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springdoc.core.customizers.OperationCustomizer")
    static class OpenApiConfig {

        /** En modo jwt, Swagger ofrece el botón "Authorize" para pegar el token de Keycloak. */
        @Bean
        @ConditionalOnProperty(name = "bancopreguntas.security.mode", havingValue = "jwt")
        org.springdoc.core.customizers.OpenApiCustomizer bearerOpenApiCustomizer() {
            return openApi -> {
                if (openApi.getComponents() == null) {
                    openApi.setComponents(new io.swagger.v3.oas.models.Components());
                }
                openApi.getComponents().addSecuritySchemes("bearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT")
                        .description("Token de acceso de Keycloak"));
                openApi.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth"));
            };
        }

        @Bean
        UserHeadersOpenApiCustomizer userHeadersOpenApiCustomizer(
                @Value("${bancopreguntas.security.mode:headers}") String modo) {
            return new UserHeadersOpenApiCustomizer("headers".equalsIgnoreCase(modo));
        }
    }
}
