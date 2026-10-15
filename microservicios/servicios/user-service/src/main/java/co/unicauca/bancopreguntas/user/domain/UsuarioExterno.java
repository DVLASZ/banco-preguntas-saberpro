package co.unicauca.bancopreguntas.user.domain;

import java.util.Set;

/** Un usuario tal como lo devuelve el proveedor de identidad (Keycloak). */
public record UsuarioExterno(String keycloakId, String username, String nombre, String correo, Set<String> roles,
                             boolean activo) {

    public UsuarioExterno {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
