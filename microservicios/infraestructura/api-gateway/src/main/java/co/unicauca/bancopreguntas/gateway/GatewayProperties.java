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

    /**
     * Los servicios detrás del gateway, con las rutas que atiende cada uno. El orden importa: cuando dos servicios
     * comparten el prefijo {@code /api/reviews}, gana el primero que coincide (assignment-service antes que
     * evaluation-service).
     */
    public static final Map<String, List<String>> SERVICIOS = servicios();

    /**
     * Servicios internos: ninguna ruta de la API llega a ellos desde fuera (solo los llaman otros servicios), pero su
     * documentación Swagger sí se publica por el gateway.
     */
    public static final List<String> INTERNOS = List.of("validation-service", "lifecycle-service");

    public GatewayProperties {
        Map<String, String> completas = new LinkedHashMap<>();
        SERVICIOS.keySet().forEach(servicio -> completas.put(servicio, "lb://" + servicio));
        INTERNOS.forEach(servicio -> completas.put(servicio, "lb://" + servicio));
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
        // la asignación de revisores va primero: solo toma sus rutas y el resto de /api/reviews es de la evaluación
        servicios.put("assignment-service", List.of("/api/reviews/config", "/api/reviews/pending-assignment",
                "/api/reviews/assigned", "/api/reviews/*/assignments"));
        servicios.put("evaluation-service", List.of("/api/reviews/**"));
        servicios.put("notification-service", List.of("/api/notifications/**"));
        servicios.put("email-service", List.of("/api/emails/**"));
        servicios.put("history-service", List.of("/api/history/**"));
        servicios.put("audit-service", List.of("/api/audit/**"));
        servicios.put("user-service", List.of("/api/users/**"));
        servicios.put("simulation-service", List.of("/api/simulations/**"));
        servicios.put("tracking-service", List.of("/api/tracking/**"));
        return java.util.Collections.unmodifiableMap(servicios);
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
