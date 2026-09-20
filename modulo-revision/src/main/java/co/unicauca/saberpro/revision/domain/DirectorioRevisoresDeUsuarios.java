package co.unicauca.saberpro.revision.domain;

import co.unicauca.saberpro.usuarios.domain.service.UserService;

import java.util.List;

/**
 * Adaptador que obtiene los revisores del módulo de usuarios.
 *
 * <p>TODO(HU-04): con {@code userService.listUsers()} devolver, como
 * {@link Revisor}, los usuarios cuyo rol sea {@code Role.REVISOR} y cuyo estado
 * sea {@code UserStatus.ACTIVO} (usa {@code getUsername()} y {@code getFullName()}).
 */
public class DirectorioRevisoresDeUsuarios implements DirectorioRevisores {

    private final UserService userService;

    public DirectorioRevisoresDeUsuarios(UserService userService) {
        this.userService = userService;
    }

    @Override
    public List<Revisor> revisoresActivos() {
        throw new UnsupportedOperationException("HU-04: implementar DirectorioRevisoresDeUsuarios");
    }
}
