package co.unicauca.bancopreguntas.user.domain;

import java.time.Instant;

/**
 * Un intento de inicio de sesión registrado por el proveedor de identidad.
 *
 * @param usuario nombre de usuario con el que se intentó (puede no existir, si el intento fue fallido)
 * @param motivo  causa del fallo, o nulo si fue exitoso
 */
public record EventoDeSesion(Instant momento, boolean exitoso, String usuario, String ip, String motivo) {

    public EventoDeSesion {
        if (momento == null) {
            throw new IllegalArgumentException("El momento del evento es obligatorio");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario del evento es obligatorio");
        }
    }
}
