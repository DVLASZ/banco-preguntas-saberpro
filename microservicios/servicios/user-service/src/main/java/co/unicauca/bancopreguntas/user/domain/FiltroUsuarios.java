package co.unicauca.bancopreguntas.user.domain;

import java.util.Locale;

/**
 * Criterios para listar usuarios; los nulos no filtran.
 *
 * @param rol          solo los que tienen este rol (por ejemplo REVIEWER para elegir revisores)
 * @param texto        parte del nombre de usuario, del nombre o del correo, sin distinguir mayúsculas
 * @param soloActivos  excluir las cuentas deshabilitadas
 */
public record FiltroUsuarios(String rol, String texto, boolean soloActivos) {

    public FiltroUsuarios {
        rol = rol == null || rol.isBlank() ? null : rol.trim().toUpperCase(Locale.ROOT);
        texto = texto == null || texto.isBlank() ? null : texto.trim();
    }

    public static FiltroUsuarios activos() {
        return new FiltroUsuarios(null, null, true);
    }
}
