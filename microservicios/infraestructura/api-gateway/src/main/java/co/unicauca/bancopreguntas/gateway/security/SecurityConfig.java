package co.unicauca.bancopreguntas.gateway.security;

import co.unicauca.bancopreguntas.gateway.GatewayProperties;
import co.unicauca.bancopreguntas.gateway.web.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Seguridad del gateway: exige un token JWT de Keycloak en todo salvo la salud, la documentación y los
 * mensajes de respaldo, aplica los roles de {@link RoleRules} y responde 401/403 con el formato de error común.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain seguridad(ServerHttpSecurity http, ObjectMapper json, CorsConfigurationSource cors,
                                     @Value("${gateway.security.user-id-claim:preferred_username}") String claimDelUsuario) {
        ServerAuthenticationEntryPoint sinCredenciales = (exchange, e) -> escribir(exchange, json, HttpStatus.UNAUTHORIZED,
                ApiError.de("NO_AUTENTICADO", "Falta un token de acceso válido"));
        ServerAccessDeniedHandler sinPermiso = (exchange, e) -> escribir(exchange, json, HttpStatus.FORBIDDEN,
                ApiError.de("ACCESO_DENEGADO", "No tienes permiso para esta operación"));

        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(c -> c.configurationSource(cors))
                .authorizeExchange(a -> {
                    a.pathMatchers(HttpMethod.OPTIONS).permitAll();
                    a.pathMatchers("/actuator/health/**", "/actuator/info", "/fallback/**", "/docs/**",
                            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**").permitAll();
                    for (RoleRules.Regla regla : RoleRules.REGLAS) {
                        String[] roles = regla.roles().toArray(String[]::new);
                        if (regla.metodo() == null) {
                            a.pathMatchers(regla.camino()).hasAnyRole(roles);
                        } else {
                            a.pathMatchers(regla.metodo(), regla.camino()).hasAnyRole(roles);
                        }
                    }
                    a.anyExchange().authenticated();
                })
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(conversor(claimDelUsuario)))
                        .authenticationEntryPoint(sinCredenciales)
                        .accessDeniedHandler(sinPermiso))
                .exceptionHandling(e -> e.authenticationEntryPoint(sinCredenciales).accessDeniedHandler(sinPermiso));
        return http.build();
    }

    /** El usuario sale de la claim indicada y los roles de realm_access.roles, como ROLE_&lt;ROL&gt;. */
    static ReactiveJwtAuthenticationConverter conversor(String claimDelUsuario) {
        ReactiveJwtAuthenticationConverter conversor = new ReactiveJwtAuthenticationConverter();
        conversor.setPrincipalClaimName(claimDelUsuario);
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object acceso = jwt.getClaim("realm_access");
            if (!(acceso instanceof Map<?, ?> mapa) || !(mapa.get("roles") instanceof List<?> roles)) {
                return Flux.empty();
            }
            return Flux.fromIterable(roles).filter(String.class::isInstance)
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(
                            "ROLE_" + ((String) r).trim().toUpperCase(Locale.ROOT)));
        });
        return conversor;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(GatewayProperties propiedades) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(propiedades.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Request-Id"));
        config.setExposedHeaders(List.of("X-Request-Id", "Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(propiedades.cors().maxAge());
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", config);
        return fuente;
    }

    private static Mono<Void> escribir(ServerWebExchange exchange, ObjectMapper json, HttpStatus estado, ApiError error) {
        exchange.getResponse().setStatusCode(estado);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            DataBuffer cuerpo = exchange.getResponse().bufferFactory().wrap(json.writeValueAsBytes(error));
            return exchange.getResponse().writeWith(Mono.just(cuerpo));
        } catch (Exception e) {
            return exchange.getResponse().setComplete();
        }
    }
}
