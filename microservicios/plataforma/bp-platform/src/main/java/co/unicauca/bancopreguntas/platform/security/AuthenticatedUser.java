package co.unicauca.bancopreguntas.platform.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * El usuario que hace la petición: su identificador y sus roles. Los controladores lo reciben como
 * parámetro y no saben de dónde sale: hoy de las cabeceras {@code X-User-Id} y {@code X-User-Roles} que
 * propaga el gateway, y con Keycloak, de las claims del token JWT.
 */
public record AuthenticatedUser(String id, Set<String> roles) {

    public AuthenticatedUser {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador del usuario es obligatorio");
        }
        id = id.trim();
        roles = roles == null ? Set.of() : roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> r.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasRole(String rol) {
        return roles.contains(rol.toUpperCase(Locale.ROOT));
    }

    /**
     * Exige que el usuario tenga al menos uno de los roles indicados.
     *
     * @throws ForbiddenException si no tiene ninguno
     */
    public void requireAnyRole(String... permitidos) {
        for (String rol : permitidos) {
            if (hasRole(rol)) {
                return;
            }
        }
        throw new ForbiddenException("Esta operación requiere uno de estos roles: " + String.join(", ", permitidos));
    }

    /** Interpreta una lista de roles separados por comas, como llega en la cabecera. */
    public static Set<String> rolesDe(String separadosPorComas) {
        if (separadosPorComas == null || separadosPorComas.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(separadosPorComas.split(",")).collect(Collectors.toUnmodifiableSet());
    }
}
