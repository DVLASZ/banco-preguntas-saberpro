package co.unicauca.bancopreguntas.platform.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthenticatedUser")
class AuthenticatedUserTest {

    @Test
    @DisplayName("normaliza el identificador y pone los roles en mayúsculas sin repetidos ni vacíos")
    void normaliza() {
        AuthenticatedUser usuario = new AuthenticatedUser("  u-1 ", Set.of("admin", " Author ", "", "ADMIN"));

        assertThat(usuario.id()).isEqualTo("u-1");
        assertThat(usuario.roles()).containsExactlyInAnyOrder("ADMIN", "AUTHOR");
    }

    @Test
    @DisplayName("el identificador es obligatorio")
    void exigeIdentificador() {
        assertThatThrownBy(() -> new AuthenticatedUser(" ", Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthenticatedUser(null, Set.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sin roles es un usuario sin permisos, no un error")
    void sinRoles() {
        assertThat(new AuthenticatedUser("u-1", null).roles()).isEmpty();
    }

    @Test
    @DisplayName("hasRole no distingue mayúsculas")
    void hasRole() {
        AuthenticatedUser usuario = new AuthenticatedUser("u-1", Set.of("REVIEWER"));

        assertThat(usuario.hasRole("reviewer")).isTrue();
        assertThat(usuario.hasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("requireAnyRole deja pasar si tiene alguno de los roles")
    void requireAnyRolePermite() {
        AuthenticatedUser usuario = new AuthenticatedUser("u-1", Set.of("REVIEWER"));

        assertThatCode(() -> usuario.requireAnyRole(Roles.ADMIN, Roles.REVIEWER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireAnyRole lanza ForbiddenException si no tiene ninguno")
    void requireAnyRoleDeniega() {
        AuthenticatedUser usuario = new AuthenticatedUser("u-1", Set.of("AUTHOR"));

        assertThatThrownBy(() -> usuario.requireAnyRole(Roles.ADMIN, Roles.REVIEWER))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("ADMIN")
                .hasMessageContaining("REVIEWER");
    }

    @Test
    @DisplayName("rolesDe interpreta una lista separada por comas")
    void rolesDe() {
        assertThat(AuthenticatedUser.rolesDe("ADMIN, author ,REVIEWER")).containsExactlyInAnyOrder("ADMIN", " author ", "REVIEWER");
        assertThat(AuthenticatedUser.rolesDe(null)).isEmpty();
        assertThat(AuthenticatedUser.rolesDe("  ")).isEmpty();
    }
}
