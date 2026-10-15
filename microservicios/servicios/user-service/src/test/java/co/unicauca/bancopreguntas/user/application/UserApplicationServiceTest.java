package co.unicauca.bancopreguntas.user.application;

import co.unicauca.bancopreguntas.user.domain.EventoDeSesion;
import co.unicauca.bancopreguntas.user.domain.FiltroUsuarios;
import co.unicauca.bancopreguntas.user.domain.Usuario;
import co.unicauca.bancopreguntas.user.domain.UsuarioExterno;
import co.unicauca.bancopreguntas.user.domain.event.LoginFailed;
import co.unicauca.bancopreguntas.user.domain.event.LoginSucceeded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserApplicationService (casos de uso)")
class UserApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    private FakeIdentityProvider keycloak;
    private InMemoryUserRepository usuarios;
    private InMemorySessionCursor cursor;
    private RecordingEventPublisher eventos;
    private RelojManual reloj;
    private UserApplicationService servicio;

    @BeforeEach
    void preparar() {
        keycloak = new FakeIdentityProvider();
        usuarios = new InMemoryUserRepository();
        cursor = new InMemorySessionCursor(T0);
        eventos = new RecordingEventPublisher();
        reloj = new RelojManual(T0.plusSeconds(1000));
        servicio = new UserApplicationService(keycloak, usuarios, cursor, eventos, reloj);
    }

    private static UsuarioExterno externo(String username, boolean activo, String... roles) {
        return new UsuarioExterno("kc-" + username, username, "Nombre " + username, username + "@correo.test",
                Set.of(roles), activo);
    }

    // ---------------------------------------------------------------- usuarios

    @Test
    @DisplayName("la primera sincronización crea los usuarios de Keycloak")
    void crea() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        keycloak.usuarios.add(externo("luis", true, "REVIEWER", "AUTHOR"));

        ResultadoDeSincronizacion r = servicio.sincronizarUsuarios();

        assertThat(r).isEqualTo(new ResultadoDeSincronizacion(2, 0, 0));
        assertThat(usuarios.buscar("luis")).get().satisfies(u -> {
            assertThat(u.roles()).containsExactlyInAnyOrder("REVIEWER", "AUTHOR");
            assertThat(u.correo()).isEqualTo("luis@correo.test");
            assertThat(u.sincronizadoEn()).isEqualTo(T0.plusSeconds(1000));
        });
    }

    @Test
    @DisplayName("sincronizar de nuevo actualiza los cambios y conserva el último inicio de sesión")
    void actualiza() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        servicio.sincronizarUsuarios();
        usuarios.guardar(usuarios.buscar("ana").orElseThrow().conInicioDeSesion(T0));
        keycloak.usuarios.set(0, externo("ana", true, "AUTHOR", "REVIEWER"));

        ResultadoDeSincronizacion r = servicio.sincronizarUsuarios();

        assertThat(r).isEqualTo(new ResultadoDeSincronizacion(0, 1, 0));
        assertThat(usuarios.buscar("ana").orElseThrow().roles()).containsExactlyInAnyOrder("AUTHOR", "REVIEWER");
        assertThat(usuarios.buscar("ana").orElseThrow().ultimoInicioDeSesion()).isEqualTo(T0);
    }

    @Test
    @DisplayName("quien Keycloak ya no devuelve queda inactivo, sin borrarse")
    void desactiva() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        keycloak.usuarios.add(externo("luis", true, "REVIEWER"));
        servicio.sincronizarUsuarios();
        keycloak.usuarios.remove(1);

        ResultadoDeSincronizacion r = servicio.sincronizarUsuarios();

        assertThat(r.desactivados()).isEqualTo(1);
        assertThat(usuarios.buscar("luis").orElseThrow().activo()).isFalse();
        assertThat(usuarios.buscar("ana").orElseThrow().activo()).isTrue();
        assertThat(servicio.sincronizarUsuarios().desactivados()).isZero(); // ya estaba inactivo
    }

    @Test
    @DisplayName("una cuenta deshabilitada en Keycloak queda inactiva")
    void deshabilitada() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        servicio.sincronizarUsuarios();
        keycloak.usuarios.set(0, externo("ana", false, "AUTHOR"));

        servicio.sincronizarUsuarios();

        assertThat(usuarios.buscar("ana").orElseThrow().activo()).isFalse();
    }

    @Test
    @DisplayName("si Keycloak responde sin usuarios habiendo directorio, no se desactiva a nadie")
    void respuestaVacia() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        servicio.sincronizarUsuarios();
        keycloak.usuarios.clear();

        ResultadoDeSincronizacion r = servicio.sincronizarUsuarios();

        assertThat(r).isEqualTo(new ResultadoDeSincronizacion(0, 0, 0));
        assertThat(usuarios.buscar("ana").orElseThrow().activo()).isTrue();
    }

    @Test
    @DisplayName("si Keycloak falla, la sincronización falla y no cambia nada")
    void keycloakCaido() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        servicio.sincronizarUsuarios();
        keycloak.caido = true;

        assertThatThrownBy(() -> servicio.sincronizarUsuarios()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> servicio.sincronizarSesiones()).isInstanceOf(IllegalStateException.class);
        assertThat(usuarios.buscar("ana").orElseThrow().activo()).isTrue();
        assertThat(cursor.valor).isEqualTo(T0);
    }

    // ---------------------------------------------------------------- sesiones

    private static EventoDeSesion exito(String usuario, int segundos) {
        return new EventoDeSesion(T0.plusSeconds(segundos), true, usuario, "10.0.0.1", null);
    }

    private static EventoDeSesion fallo(String usuario, int segundos) {
        return new EventoDeSesion(T0.plusSeconds(segundos), false, usuario, "10.0.0.9", "invalid_user_credentials");
    }

    @Test
    @DisplayName("publica los inicios de sesión exitosos y fallidos, en orden, y adelanta el cursor")
    void publica() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        servicio.sincronizarUsuarios();
        keycloak.eventos.add(fallo("intruso", 20));
        keycloak.eventos.add(exito("ana", 10));

        int publicados = servicio.sincronizarSesiones();

        assertThat(publicados).isEqualTo(2);
        assertThat(eventos.publicados()).containsExactly(
                new LoginSucceeded("ana", "10.0.0.1", T0.plusSeconds(10)),
                new LoginFailed("intruso", "10.0.0.9", "invalid_user_credentials", T0.plusSeconds(20)));
        assertThat(cursor.valor).isEqualTo(T0.plusSeconds(20));
        assertThat(keycloak.ultimaConsultaDesde).isEqualTo(T0);
    }

    @Test
    @DisplayName("el inicio de sesión exitoso actualiza el último acceso del usuario")
    void ultimoAcceso() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        servicio.sincronizarUsuarios();
        keycloak.eventos.add(exito("ana", 10));
        keycloak.eventos.add(exito("ana", 30));

        servicio.sincronizarSesiones();

        assertThat(usuarios.buscar("ana").orElseThrow().ultimoInicioDeSesion()).isEqualTo(T0.plusSeconds(30));
    }

    @Test
    @DisplayName("un inicio de sesión de alguien aún no sincronizado se publica igual")
    void usuarioDesconocido() {
        keycloak.eventos.add(exito("recien-llegado", 10));

        assertThat(servicio.sincronizarSesiones()).isEqualTo(1);
        assertThat(usuarios.buscar("recien-llegado")).isEmpty();
    }

    @Test
    @DisplayName("no vuelve a publicar lo que ya publicó")
    void sinRepetir() {
        keycloak.eventos.add(exito("ana", 10));

        servicio.sincronizarSesiones();
        int segunda = servicio.sincronizarSesiones();

        assertThat(segunda).isZero();
        assertThat(eventos.publicados()).hasSize(1);
    }

    @Test
    @DisplayName("ignora los eventos que no son posteriores al cursor")
    void soloNuevos() {
        keycloak.eventos.add(exito("ana", 0));
        keycloak.eventos.add(new EventoDeSesion(T0.minusSeconds(5), true, "ana", null, null));

        assertThat(servicio.sincronizarSesiones()).isZero();
        assertThat(cursor.valor).isEqualTo(T0);
    }

    @Test
    @DisplayName("si otra réplica tiene el cursor, esta no hace nada")
    void otraReplica() {
        keycloak.eventos.add(exito("ana", 10));
        cursor.tomadoPorOtraReplica = true;

        assertThat(servicio.sincronizarSesiones()).isZero();
        assertThat(eventos.publicados()).isEmpty();
        assertThat(keycloak.ultimaConsultaDesde).isNull();
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("lista los revisores activos para elegirlos")
    void revisores() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        keycloak.usuarios.add(externo("luis", true, "REVIEWER"));
        keycloak.usuarios.add(externo("marta", true, "REVIEWER"));
        keycloak.usuarios.add(externo("pedro", false, "REVIEWER"));
        servicio.sincronizarUsuarios();

        var activos = servicio.listar(new FiltroUsuarios("REVIEWER", null, true), 1, 10);
        var todos = servicio.listar(new FiltroUsuarios("reviewer", null, false), 1, 10);
        var buscados = servicio.listar(new FiltroUsuarios(null, "mart", true), 1, 10);

        assertThat(activos.elementos()).extracting(Usuario::id).containsExactly("luis", "marta");
        assertThat(todos.totalElementos()).isEqualTo(3);
        assertThat(buscados.elementos()).extracting(Usuario::id).containsExactly("marta");
    }

    @Test
    @DisplayName("pedir una página que no existe devuelve la última; los parámetros se validan")
    void paginacion() {
        for (int i = 0; i < 3; i++) {
            keycloak.usuarios.add(externo("u" + i, true, "AUTHOR"));
        }
        servicio.sincronizarUsuarios();

        var pagina = servicio.listar(FiltroUsuarios.activos(), 9, 2);

        assertThat(pagina.numero()).isEqualTo(2);
        assertThat(pagina.elementos()).hasSize(1);
        assertThatThrownBy(() -> servicio.listar(FiltroUsuarios.activos(), 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.listar(FiltroUsuarios.activos(), 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el perfil de un usuario sincronizado se encuentra y el de uno desconocido no")
    void perfil() {
        keycloak.usuarios.add(externo("ana", true, "AUTHOR"));
        servicio.sincronizarUsuarios();

        assertThat(servicio.perfil("ana")).isPresent();
        assertThat(servicio.perfil("nadie")).isEmpty();
    }
}
