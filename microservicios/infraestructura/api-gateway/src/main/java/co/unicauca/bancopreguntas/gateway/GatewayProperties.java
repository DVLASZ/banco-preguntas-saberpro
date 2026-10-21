package co.unicauca.bancopreguntas.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración propia del gateway.
 *
 * @param uris  a dónde enviar cada servicio; por defecto {@code lb://<servicio>}, que reparte entre las réplicas
 *              registradas en Eureka. En las pruebas apuntan a servidores locales.
 * @param cors  orígenes del navegador que pueden llamar al gateway
 * @param timeout cuánto espera una respuesta de un servicio antes de dar el circuito por fallido
 */
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(Map<String, String> uris, Cors cors, Duration timeout) {

    /** Los servicios detrás del gateway, con las rutas que atiende cada uno. */
    public static final Map<String, List<String>> SERVICIOS = servicios();

    public GatewayProperties {
        Map<String, String> completas = new LinkedHashMap<>();
        SERVICIOS.keySet().forEach(servicio -> completas.put(servicio, "lb://" + servicio));
        if (uris != null) {
            completas.putAll(uris);
        }
        uris = Map.copyOf(completas);
        cors = cors == null ? new Cors(null, null) : cors;
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
    }

    private static Map<String, List<String>> servicios() {
        Map<String, List<String>> servicios = new LinkedHashMap<>();
        servicios.put("question-service", List.of("/api/questions/**"));
        servicios.put("review-service", List.of("/api/reviews/**"));
        servicios.put("notification-service", List.of("/api/notifications/**"));
        servicios.put("history-service", List.of("/api/history/**"));
        servicios.put("user-service", List.of("/api/users/**"));
        servicios.put("simulation-service", List.of("/api/simulations/**"));
        servicios.put("tracking-service", List.of("/api/tracking/**"));
        return Map.copyOf(servicios);
    }

    /** Orígenes permitidos (por defecto la aplicación Angular en desarrollo) y cuánto cachea el navegador el permiso. */
    public record Cors(List<String> allowedOrigins, Duration maxAge) {

        public Cors {
            allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
                    ? List.of("http://localhost:4200") : List.copyOf(allowedOrigins);
            maxAge = maxAge == null ? Duration.ofHours(1) : maxAge;
        }
    }
}
