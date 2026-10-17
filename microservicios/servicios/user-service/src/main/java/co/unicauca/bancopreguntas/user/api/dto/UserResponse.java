package co.unicauca.bancopreguntas.user.api.dto;

import co.unicauca.bancopreguntas.user.domain.Usuario;

import java.time.Instant;
import java.util.List;
import java.util.TreeSet;

/** Un usuario del directorio. */
public record UserResponse(String id, String nombre, String correo, List<String> roles, boolean activo,
                           Instant ultimoInicioDeSesion) {

    public static UserResponse de(Usuario u) {
        return new UserResponse(u.id(), u.nombre(), u.correo(), List.copyOf(new TreeSet<>(u.roles())), u.activo(),
                u.ultimoInicioDeSesion());
    }
}
