package co.unicauca.bancopreguntas.gateway.config;

import co.unicauca.bancopreguntas.gateway.GatewayProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4jBulkheadProvider;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4jBulkheadConfigurationBuilder;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Duration;

/**
 * Las rutas del gateway: cada servicio atiende sus caminos {@code /api/...}, y su documentación Swagger se
 * publica en {@code /docs/<servicio>/v3/api-docs} para verlas todas juntas. Los servicios internos (validación y ciclo de
 * vida) no tienen rutas de API, solo documentación. Cada ruta tiene un circuit breaker
 * (si el servicio no responde, se contesta 503 de inmediato en lugar de dejar esperando) y las consultas GET se
 * reintentan si una réplica falla.
 */
@Configuration
public class GatewayRoutesConfig {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayRoutesConfig.class);

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder, GatewayProperties propiedades) {
        RouteLocatorBuilder.Builder rutas = builder.routes();
        GatewayProperties.SERVICIOS.forEach((servicio, caminos) -> {
            String destino = propiedades.uris().get(servicio);
            rutas.route(servicio, r -> r.path(caminos.toArray(String[]::new))
                    .filters(f -> conResiliencia(f, servicio))
                    .uri(destino));
            rutas.route(servicio + "-docs", r -> r.path("/docs/" + servicio + "/v3/api-docs")
                    .filters(f -> f.rewritePath("/docs/" + servicio + "/v3/api-docs", "/v3/api-docs"))
                    .uri(destino));
        });
        GatewayProperties.INTERNOS.forEach(servicio -> rutas.route(servicio + "-docs",
                r -> r.path("/docs/" + servicio + "/v3/api-docs")
                        .filters(f -> f.rewritePath("/docs/" + servicio + "/v3/api-docs", "/v3/api-docs"))
                        .uri(propiedades.uris().get(servicio))));
        return rutas.build();
    }

    private static GatewayFilterSpec conResiliencia(GatewayFilterSpec f, String servicio) {
        // El circuit breaker va por fuera y el reintento por dentro: si tras reintentar sigue sin haber respuesta,
        // el breaker contesta con el respaldo. Al revés, el reintento actuaría sobre una respuesta ya enviada.
        return f
                .circuitBreaker(c -> c.setName(servicio).setFallbackUri("forward:/fallback/" + servicio))
                .retry(r -> r.setRetries(2)
                        .setMethods(HttpMethod.GET)
                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT)
                        .setBackoff(Duration.ofMillis(50), Duration.ofMillis(500), 2, true));
    }

    /** Valores del circuit breaker de todas las rutas. */
    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerPorDefecto(GatewayProperties propiedades) {
        return fabrica -> {
            registrarEventos(fabrica);
            configurar(fabrica, propiedades);
        };
    }

    /**
     * Cuántas llamadas simultáneas admite el gateway hacia cada servicio. El valor por defecto de Resilience4J (25) es
     * muy bajo para un gateway: con carga rechazaba llamadas sanas y eso abría el circuit breaker. Se deja un tope
     * alto que sigue protegiendo al gateway si un servicio se cuelga y acumula peticiones.
     */
    @Bean
    Customizer<ReactiveResilience4jBulkheadProvider> bulkheadPorDefecto() {
        return proveedor -> proveedor.configureDefault(id -> new Resilience4jBulkheadConfigurationBuilder()
                .bulkheadConfig(BulkheadConfig.custom()
                        .maxConcurrentCalls(1000)
                        .maxWaitDuration(Duration.ofMillis(200))
                        .build())
                .build());
    }

    /** Deja en el registro cada cambio de estado del circuit breaker y los errores que lo provocan. */
    private static void registrarEventos(ReactiveResilience4JCircuitBreakerFactory fabrica) {
        fabrica.getCircuitBreakerRegistry().getEventPublisher().onEntryAdded(entrada -> {
            io.github.resilience4j.circuitbreaker.CircuitBreaker breaker = entrada.getAddedEntry();
            breaker.getEventPublisher()
                    .onStateTransition(e -> LOG.warn("Circuit breaker {}: {}", e.getCircuitBreakerName(), e.getStateTransition()))
                    .onError(e -> LOG.debug("Circuit breaker {}: error {} tras {} ms", e.getCircuitBreakerName(),
                            e.getThrowable().toString(), e.getElapsedDuration().toMillis()));
        });
    }

    private static void configurar(ReactiveResilience4JCircuitBreakerFactory fabrica, GatewayProperties propiedades) {
        fabrica.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(propiedades.timeout()).build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .build());
    }

}
