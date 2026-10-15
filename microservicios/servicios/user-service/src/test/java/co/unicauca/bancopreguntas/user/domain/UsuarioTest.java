package co.unicauca.bancopreguntas.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Usuario y criterios del directorio")
class UsuarioTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    private static UsuarioExterno externo(String username, Set<String> roles, boolean activo) {
        return new UsuarioExterno("kc-1", username, "Ana Pérez", "ana@correo.test", roles, activo);
    }

    @Test
    @DisplayName("los roles se normalizan a mayúsculas, sin vacíos, y el nombre por defecto es el usuario")
    void normaliza() {
        Usuario u = new Usuario(" ana ", "kc-1", " ", null, Set.of("reviewer", " Author ", ""), true, null, T0);

        assertThat(u.id()).isEqualTo("ana");
        assertThat(u.nombre()).isEqualTo("ana");
        assertThat(u.roles()).containsExactlyInAnyOrder("REVIEWER", "AUTHOR");
        assertThat(u.tieneRol("reviewer")).isTrue();
        assertThat(u.tieneRol("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("exige nombre de usuario")
    void obligatorio() {
        assertThatThrownBy(() -> new Usuario(" ", null, "x", null, Set.of(), true, null, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UsuarioExterno("kc", null, "x", null, Set.of(), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un usuario nuevo nace de lo que dice Keycloak y sin inicio de sesión conocido")
    void nuevo() {
        Usuario u = Usuario.de(externo("ana", Set.of("AUTHOR"), true), T0);

        assertThat(u.id()).isEqualTo("ana");
        assertThat(u.keycloakId()).isEqualTo("kc-1");
        assertThat(u.nombre()).isEqualTo("Ana Pérez");
        assertThat(u.correo()).isEqualTo("ana@correo.test");
        assertThat(u.activo()).isTrue();
        assertThat(u.ultimoInicioDeSesion()).isNull();
        assertThat(u.sincronizadoEn()).isEqualTo(T0);
    }

    @Test
    @DisplayName("actualizarlo con Keycloak cambia sus datos pero conserva su último inicio de sesión")
    void actualiza() {
        Usuario u = Usuario.de(externo("ana", Set.of("AUTHOR"), true), T0).conInicioDeSesion(T0);

        Usuario nuevo = u.actualizadoCon(new UsuarioExterno("kc-2", "ana", "Ana María", null, Set.of("REVIEWER"), false), T1);

        assertThat(nuevo.nombre()).isEqualTo("Ana María");
        assertThat(nuevo.roles()).containsExactly("REVIEWER");
        assertThat(nuevo.activo()).isFalse();
        assertThat(nuevo.keycloakId()).isEqualTo("kc-2");
        assertThat(nuevo.ultimoInicioDeSesion()).isEqualTo(T0);
        assertThat(nuevo.sincronizadoEn()).isEqualTo(T1);
    }

    @Test
    @DisplayName("el último inicio de sesión solo avanza")
    void inicioDeSesion() {
        Usuario u = Usuario.de(externo("ana", Set.of(), true), T0);

        Usuario primero = u.conInicioDeSesion(T1);
        Usuario antiguo = primero.conInicioDeSesion(T0);

        assertThat(primero.ultimoInicioDeSesion()).isEqualTo(T1);
        assertThat(antiguo.ultimoInicioDeSesion()).isEqualTo(T1);
    }

    @Test
    @DisplayName("desactivar conserva el resto de los datos")
    void inactivo() {
        Usuario u = Usuario.de(externo("ana", Set.of("AUTHOR"), true), T0).inactivo(T1);

        assertThat(u.activo()).isFalse();
        assertThat(u.roles()).containsExactly("AUTHOR");
        assertThat(u.sincronizadoEn()).isEqualTo(T1);
    }

    @Test
    @DisplayName("el filtro normaliza el rol y descarta los textos vacíos")
    void filtro() {
        FiltroUsuarios f = new FiltroUsuarios(" reviewer ", "  ", true);

        assertThat(f.rol()).isEqualTo("REVIEWER");
        assertThat(f.texto()).isNull();
        assertThat(FiltroUsuarios.activos()).isEqualTo(new FiltroUsuarios(null, null, true));
    }

    @Test
    @DisplayName("un evento de sesión exige momento y usuario")
    void evento() {
        assertThatThrownBy(() -> new EventoDeSesion(null, true, "ana", null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventoDeSesion(T0, true, " ", null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new EventoDeSesion(T0, false, "ana", "10.0.0.1", "invalid_user_credentials").motivo())
                .isEqualTo("invalid_user_credentials");
    }
}
