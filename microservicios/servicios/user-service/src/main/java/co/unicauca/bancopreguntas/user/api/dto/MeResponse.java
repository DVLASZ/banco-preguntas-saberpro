package co.unicauca.bancopreguntas.user.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Quién soy: el usuario del token y, si ya está en el directorio, su perfil. {@code sincronizado} es falso
 * cuando solo se conoce lo que trae el token (el usuario es muy reciente).
 */
public record MeResponse(String id, String nombre, String correo, List<String> roles, boolean sincronizado,
                         Instant ultimoInicioDeSesion) {
}
