package co.unicauca.bancopreguntas.user.api;

import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import co.unicauca.bancopreguntas.user.application.ResultadoDeSincronizacion;
import co.unicauca.bancopreguntas.user.application.UserApplicationService;
import co.unicauca.bancopreguntas.user.domain.FiltroUsuarios;
import co.unicauca.bancopreguntas.user.domain.Pagina;
import co.unicauca.bancopreguntas.user.domain.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("UserController (API REST)")
class UserControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserApplicationService servicio;

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder p, String usuario, String roles) {
        return p.header("X-User-Id", usuario).header("X-User-Roles", roles);
    }

    private static Usuario usuario(String id, String... roles) {
        return new Usuario(id, "kc-" + id, "Nombre " + id, id + "@correo.test", Set.of(roles), true, T0, T0);
    }

    @Test
    @DisplayName("GET / lista los usuarios con los filtros recibidos y paginación desde 0 (ADMIN)")
    void listar() throws Exception {
        when(servicio.listar(new FiltroUsuarios("REVIEWER", "ana", true), 2, 5))
                .thenReturn(new Pagina<>(List.of(usuario("ana", "REVIEWER", "AUTHOR")), 2, 5, 6));

        mvc.perform(como(get("/api/users?rol=reviewer&texto=ana&page=1&size=5"), "admin-1", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value("ana"))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Nombre ana"))
                .andExpect(jsonPath("$.contenido[0].roles[0]").value("AUTHOR"))
                .andExpect(jsonPath("$.contenido[0].roles[1]").value("REVIEWER"))
                .andExpect(jsonPath("$.contenido[0].activo").value(true))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.totalElementos").value(6));
    }

    @Test
    @DisplayName("por defecto lista solo las cuentas activas; soloActivos=false las incluye todas")
    void soloActivos() throws Exception {
        when(servicio.listar(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new Pagina<>(List.of(), 1, 20, 0));

        mvc.perform(como(get("/api/users"), "admin-1", "ADMIN")).andExpect(status().isOk());
        mvc.perform(como(get("/api/users?soloActivos=false"), "admin-1", "ADMIN")).andExpect(status().isOk());

        verify(servicio).listar(new FiltroUsuarios(null, null, true), 1, 20);
        verify(servicio).listar(new FiltroUsuarios(null, null, false), 1, 20);
    }

    @Test
    @DisplayName("listar es solo del administrador; sin identidad, 401")
    void listarSoloAdmin() throws Exception {
        mvc.perform(como(get("/api/users"), "autor-1", "AUTHOR")).andExpect(status().isForbidden());
        mvc.perform(como(get("/api/users"), "rev-1", "REVIEWER")).andExpect(status().isForbidden());
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("valida la paginación")
    void paginacion() throws Exception {
        mvc.perform(como(get("/api/users?page=-1"), "admin-1", "ADMIN")).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/users?size=0"), "admin-1", "ADMIN")).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/users?size=101"), "admin-1", "ADMIN")).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("GET /me devuelve mi perfil sincronizado con los roles de mi token")
    void yo() throws Exception {
        when(servicio.perfil("ana")).thenReturn(Optional.of(usuario("ana", "AUTHOR")));

        mvc.perform(como(get("/api/users/me"), "ana", "AUTHOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ana"))
                .andExpect(jsonPath("$.nombre").value("Nombre ana"))
                .andExpect(jsonPath("$.correo").value("ana@correo.test"))
                .andExpect(jsonPath("$.roles[0]").value("AUTHOR"))
                .andExpect(jsonPath("$.sincronizado").value(true));
    }

    @Test
    @DisplayName("GET /me de un usuario muy reciente responde con lo que trae el token")
    void yoSinSincronizar() throws Exception {
        when(servicio.perfil("nuevo")).thenReturn(Optional.empty());

        mvc.perform(como(get("/api/users/me"), "nuevo", "REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("nuevo"))
                .andExpect(jsonPath("$.nombre").value("nuevo"))
                .andExpect(jsonPath("$.roles[0]").value("REVIEWER"))
                .andExpect(jsonPath("$.sincronizado").value(false));
    }

    @Test
    @DisplayName("GET /me exige un rol de la plataforma")
    void yoSinRol() throws Exception {
        mvc.perform(como(get("/api/users/me"), "est-1", "STUDENT")).andExpect(status().isForbidden());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /sync sincroniza usuarios e inicios de sesión y resume lo ocurrido (ADMIN)")
    void sincronizar() throws Exception {
        when(servicio.sincronizarUsuarios()).thenReturn(new ResultadoDeSincronizacion(2, 5, 1));
        when(servicio.sincronizarSesiones()).thenReturn(3);

        mvc.perform(como(post("/api/users/sync"), "admin-1", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuariosCreados").value(2))
                .andExpect(jsonPath("$.usuariosActualizados").value(5))
                .andExpect(jsonPath("$.usuariosDesactivados").value(1))
                .andExpect(jsonPath("$.iniciosDeSesionPublicados").value(3));
    }

    @Test
    @DisplayName("sincronizar es solo del administrador")
    void sincronizarSoloAdmin() throws Exception {
        mvc.perform(como(post("/api/users/sync"), "autor-1", "AUTHOR")).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("si Keycloak no responde al sincronizar, 502 con un mensaje claro")
    void keycloakCaido() throws Exception {
        when(servicio.sincronizarUsuarios()).thenThrow(new RestClientException("sin conexión"));

        mvc.perform(como(post("/api/users/sync"), "admin-1", "ADMIN"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value("PROVEEDOR_DE_IDENTIDAD_NO_DISPONIBLE"));
    }
}
