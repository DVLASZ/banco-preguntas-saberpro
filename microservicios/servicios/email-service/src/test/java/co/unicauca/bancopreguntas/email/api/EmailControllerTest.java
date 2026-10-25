package co.unicauca.bancopreguntas.email.api;

import co.unicauca.bancopreguntas.email.application.EmailApplicationService;
import co.unicauca.bancopreguntas.email.domain.Correo;
import co.unicauca.bancopreguntas.email.domain.CorreoEnviado;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;
import co.unicauca.bancopreguntas.email.domain.Pagina;
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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("EmailController (API REST)")
class EmailControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmailApplicationService servicio;

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder p, String usuario, String roles) {
        return p.header("X-User-Id", usuario).header("X-User-Roles", roles);
    }

    private static CorreoEnviado correo(EstadoCorreo estado) {
        Correo c = new Correo("autor-1@correo.test", "Pregunta rechazada", "Cuerpo");
        return estado == EstadoCorreo.ENVIADO ? CorreoEnviado.enviado("c-1", "n-1", "autor-1", c, T0)
                : CorreoEnviado.fallido("c-1", "n-1", "autor-1", c, "servidor caído", T0);
    }

    @Test
    @DisplayName("GET /api/emails lista los correos con paginación desde 0")
    void lista() throws Exception {
        when(servicio.correos(null, 1, 10)).thenReturn(new Pagina<>(List.of(correo(EstadoCorreo.ENVIADO)), 1, 10, 1));

        mvc.perform(como(get("/api/emails"), "admin-1", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].para").value("autor-1@correo.test"))
                .andExpect(jsonPath("$.contenido[0].asunto").value("Pregunta rechazada"))
                .andExpect(jsonPath("$.contenido[0].cuerpo").value("Cuerpo"))
                .andExpect(jsonPath("$.contenido[0].estado").value("ENVIADO"))
                .andExpect(jsonPath("$.contenido[0].notificacionId").value("n-1"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("el filtro por estado llega al servicio y muestra el motivo del fallo")
    void filtro() throws Exception {
        when(servicio.correos(EstadoCorreo.FALLIDO, 2, 5)).thenReturn(new Pagina<>(List.of(correo(EstadoCorreo.FALLIDO)), 2, 5, 6));

        mvc.perform(como(get("/api/emails?estado=FALLIDO&page=1&size=5"), "admin-1", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("FALLIDO"))
                .andExpect(jsonPath("$.contenido[0].error").value("servidor caído"))
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    @DisplayName("solo el administrador consulta los correos; sin identidad responde 401")
    void seguridad() throws Exception {
        mvc.perform(como(get("/api/emails"), "autor-1", "AUTHOR")).andExpect(status().isForbidden());
        mvc.perform(get("/api/emails")).andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("un estado o un tamaño de página inválidos responden 400")
    void validacion() throws Exception {
        mvc.perform(como(get("/api/emails?estado=OTRO"), "admin-1", "ADMIN")).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/emails?size=0"), "admin-1", "ADMIN")).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/emails?size=101"), "admin-1", "ADMIN")).andExpect(status().isBadRequest());
    }
}
