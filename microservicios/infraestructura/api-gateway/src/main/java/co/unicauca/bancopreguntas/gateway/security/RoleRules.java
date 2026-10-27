package co.unicauca.bancopreguntas.gateway.security;

import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * Qué rol exige el gateway en cada ruta. Es la primera barrera: cada servicio vuelve a comprobar el rol de
 * cada operación (autorización redundante), así que un fallo aquí no abre nada. El orden importa: gana la
 * primera regla que coincide, por eso van las más específicas primero.
 */
public final class RoleRules {

    private static final String ADMIN = "ADMIN";
    private static final String AUTHOR = "AUTHOR";
    private static final String REVIEWER = "REVIEWER";
    private static final String TEACHER = "TEACHER";
    private static final String STUDENT = "STUDENT";

    /** @param metodo {@code null} para cualquier método */
    public record Regla(HttpMethod metodo, String camino, List<String> roles) {
    }

    public static final List<Regla> REGLAS = List.of(
            // usuarios
            regla(HttpMethod.GET, "/api/users/me", ADMIN, AUTHOR, REVIEWER, TEACHER, STUDENT),
            regla(null, "/api/users/**", ADMIN),
            // historial de las preguntas, auditoría y registro de correos
            regla(HttpMethod.GET, "/api/history/**", ADMIN, AUTHOR, REVIEWER),
            regla(HttpMethod.GET, "/api/audit/**", ADMIN),
            regla(HttpMethod.GET, "/api/emails/**", ADMIN),
            // notificaciones
            regla(null, "/api/notifications/**", ADMIN, AUTHOR, REVIEWER),
            // revisión
            regla(HttpMethod.GET, "/api/reviews/pending-assignment", ADMIN),
            regla(HttpMethod.POST, "/api/reviews/*/assignments", ADMIN),
            regla(HttpMethod.GET, "/api/reviews/assigned", REVIEWER),
            regla(HttpMethod.POST, "/api/reviews/*/evaluations", REVIEWER),
            regla(HttpMethod.GET, "/api/reviews/**", ADMIN, AUTHOR, REVIEWER),
            // simulacros (el estudiante los genera y los presenta; docentes y administradores los consultan)
            regla(HttpMethod.GET, "/api/simulations/catalog/summary", ADMIN, TEACHER, STUDENT),
            regla(HttpMethod.GET, "/api/simulations/**", ADMIN, TEACHER, STUDENT),
            regla(null, "/api/simulations/**", STUDENT),
            // seguimiento académico (lo propio es del estudiante; los reportes, de docentes y administradores)
            regla(HttpMethod.GET, "/api/tracking/me/**", STUDENT),
            regla(HttpMethod.GET, "/api/tracking/**", ADMIN, TEACHER),
            // preguntas
            regla(HttpMethod.GET, "/api/questions/all", ADMIN),
            regla(HttpMethod.GET, "/api/questions/pending-review", ADMIN),
            regla(HttpMethod.POST, "/api/questions/*/publish", ADMIN),
            regla(HttpMethod.GET, "/api/questions/*", ADMIN, AUTHOR, REVIEWER),
            regla(null, "/api/questions", AUTHOR),
            regla(null, "/api/questions/**", AUTHOR));

    private RoleRules() {
    }

    private static Regla regla(HttpMethod metodo, String camino, String... roles) {
        return new Regla(metodo, camino, List.of(roles));
    }
}
