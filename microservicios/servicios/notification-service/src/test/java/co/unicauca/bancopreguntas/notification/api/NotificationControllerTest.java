package co.unicauca.bancopreguntas.notification.api;

import co.unicauca.bancopreguntas.notification.application.NotificationApplicationService;
import co.unicauca.bancopreguntas.notification.domain.NotificacionNoEncontradaException;
import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.Pagina;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;
import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("NotificationController (API REST)")
class NotificationControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NotificationApplicationService servicio;

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder p, String usuario, String roles) {
        return p.header("X-User-Id", usuario).header("X-User-Roles", roles);
    }

    private static MockHttpServletRequestBuilder comoAutor(MockHttpServletRequestBuilder p) {
        return como(p, "autor-1", "AUTHOR");
    }

    private static Notificacion notificacion() {
        return Notificacion.nueva("n-1", "autor-1", "autor-1@correo.test", TipoNotificacion.REVISION_RECHAZADA, "q-1",
                new Mensaje("Pregunta rechazada", "Cuerpo"), T0).conCorreoEnviado(T0);
    }

    @Test
    @DisplayName("GET / devuelve mi bandeja con paginación desde 0")
    void bandeja() throws Exception {
        when(servicio.bandeja("autor-1", false, 1, 10)).thenReturn(new Pagina<>(List.of(notificacion()), 1, 10, 1));

        mvc.perform(comoAutor(get("/api/notifications")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value("n-1"))
                .andExpect(jsonPath("$.contenido[0].tipo").value("REVISION_RECHAZADA"))
                .andExpect(jsonPath("$.contenido[0].asunto").value("Pregunta rechazada"))
                .andExpect(jsonPath("$.contenido[0].leida").value(false))
                .andExpect(jsonPath("$.contenido[0].correoEnviadoEn").exists())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("acepta el filtro de no leídas y la paginación")
    void filtros() throws Exception {
        when(servicio.bandeja("autor-1", true, 3, 5)).thenReturn(new Pagina<>(List.of(), 3, 5, 0));

        mvc.perform(comoAutor(get("/api/notifications?soloNoLeidas=true&page=2&size=5"))).andExpect(status().isOk());

        verify(servicio).bandeja("autor-1", true, 3, 5);
    }

    @Test
    @DisplayName("valida la paginación")
    void paginacionInvalida() throws Exception {
        mvc.perform(comoAutor(get("/api/notifications?page=-1"))).andExpect(status().isBadRequest());
        mvc.perform(comoAutor(get("/api/notifications?size=0"))).andExpect(status().isBadRequest());
        mvc.perform(comoAutor(get("/api/notifications?size=101"))).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("sin identidad responde 401 y con otro rol, 403")
    void seguridad() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(como(get("/api/notifications"), "est-1", "STUDENT")).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("cualquier rol de la plataforma tiene bandeja")
    void todosLosRoles() throws Exception {
        when(servicio.bandeja("u-1", false, 1, 10)).thenReturn(new Pagina<>(List.of(), 1, 10, 0));

        mvc.perform(como(get("/api/notifications"), "u-1", "ADMIN")).andExpect(status().isOk());
        mvc.perform(como(get("/api/notifications"), "u-1", "REVIEWER")).andExpect(status().isOk());
        mvc.perform(como(get("/api/notifications"), "u-1", "AUTHOR")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /unread-count informa cuántas hay sin leer")
    void sinLeer() throws Exception {
        when(servicio.noLeidas("autor-1")).thenReturn(4L);

        mvc.perform(comoAutor(get("/api/notifications/unread-count")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(4));
    }

    @Test
    @DisplayName("POST /{id}/read marca una notificación mía como leída")
    void marcarLeida() throws Exception {
        when(servicio.marcarComoLeida("n-1", "autor-1")).thenReturn(notificacion().marcadaComoLeida(T0.plusSeconds(5)));

        mvc.perform(comoAutor(post("/api/notifications/n-1/read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));
    }

    @Test
    @DisplayName("leer una notificación ajena o inexistente responde 404")
    void ajena() throws Exception {
        when(servicio.marcarComoLeida("n-9", "autor-1")).thenThrow(new NotificacionNoEncontradaException("n-9"));

        mvc.perform(comoAutor(post("/api/notifications/n-9/read")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NOTIFICACION_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("POST /read-all marca todas mis notificaciones como leídas")
    void marcarTodas() throws Exception {
        when(servicio.marcarTodasComoLeidas("autor-1")).thenReturn(3);

        mvc.perform(comoAutor(post("/api/notifications/read-all")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcadas").value(3));
    }

    @Test
    @DisplayName("GET /emails lista los correos simulados y es solo del administrador")
    void correos() throws Exception {
        when(servicio.correosEnviados(1, 10)).thenReturn(new Pagina<>(List.of(notificacion()), 1, 10, 1));

        mvc.perform(como(get("/api/notifications/emails"), "admin-1", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].para").value("autor-1@correo.test"))
                .andExpect(jsonPath("$.contenido[0].asunto").value("Pregunta rechazada"))
                .andExpect(jsonPath("$.contenido[0].cuerpo").value("Cuerpo"));

        mvc.perform(comoAutor(get("/api/notifications/emails"))).andExpect(status().isForbidden());
    }
}
