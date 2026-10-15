package co.unicauca.bancopreguntas.user.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Un usuario del banco de preguntas, tal como se conoce en este servicio: una copia de lo que Keycloak sabe de
 * él. {@code id} es el nombre de usuario, el mismo que usan los demás servicios para identificarlo.
 *
 * @param keycloakId          identificador interno de Keycloak (el {@code sub} del token)
 * @param roles               ADMIN, AUTHOR, REVIEWER... en mayúsculas
 * @param activo              si la cuenta sigue habilitada; si Keycloak ya no la devuelve, se marca inactiva
 * @param ultimoInicioDeSesion cuándo inició sesión por última vez, o nulo si no se ha visto
 */
public record Usuario(String id, String keycloakId, String nombre, String correo, Set<String> roles, boolean activo,
                      Instant ultimoInicioDeSesion, Instant sincronizadoEn) {

    public Usuario {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        id = id.trim();
        roles = roles == null ? Set.of() : roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> r.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        nombre = nombre == null || nombre.isBlank() ? id : nombre.trim();
    }

    public boolean tieneRol(String rol) {
        return roles.contains(rol.toUpperCase(Locale.ROOT));
    }

    /** El usuario tal como lo reporta ahora Keycloak, conservando su último inicio de sesión conocido. */
    public Usuario actualizadoCon(UsuarioExterno externo, Instant ahora) {
        return new Usuario(id, externo.keycloakId(), externo.nombre(), externo.correo(), externo.roles(),
                externo.activo(), ultimoInicioDeSesion, ahora);
    }

    public Usuario inactivo(Instant ahora) {
        return new Usuario(id, keycloakId, nombre, correo, roles, false, ultimoInicioDeSesion, ahora);
    }

    public Usuario conInicioDeSesion(Instant momento) {
        Instant ultimo = ultimoInicioDeSesion == null || momento.isAfter(ultimoInicioDeSesion) ? momento : ultimoInicioDeSesion;
        return new Usuario(id, keycloakId, nombre, correo, roles, activo, ultimo, sincronizadoEn);
    }

    public static Usuario de(UsuarioExterno externo, Instant ahora) {
        return new Usuario(externo.username(), externo.keycloakId(), externo.nombre(), externo.correo(),
                externo.roles(), externo.activo(), null, ahora);
    }
}
