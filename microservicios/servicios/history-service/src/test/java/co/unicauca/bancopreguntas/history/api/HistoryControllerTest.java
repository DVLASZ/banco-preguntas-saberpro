package co.unicauca.bancopreguntas.history.api;

import co.unicauca.bancopreguntas.history.application.HistoryApplicationService;
import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.history.domain.HistorialNoEncontradoException;
import co.unicauca.bancopreguntas.history.domain.Observador;
import co.unicauca.bancopreguntas.history.domain.Pagina;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HistoryController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("HistoryController (API REST)")
class HistoryControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HistoryApplicationService servicio;

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder p, String usuario, String roles) {
        return p.header("X-User-Id", usuario).header("X-User-Roles", roles);
    }

    private static EntradaDeHistorial entrada() {
        return new EntradaDeHistorial("h-1", "e-1", "q-1", "autor-1", TipoEntrada.PREGUNTA_ENVIADA,
                "autor-1 envió a revisión «Observer» (versión 1)", "{\"questionId\":\"q-1\",\"version\":1}", T0, T0);
    }

    @Test
    @DisplayName("GET /questions/{id} devuelve el rastro con el detalle del evento como JSON")
    void deUnaPregunta() throws Exception {
        when(servicio.lineaDeTiempo("q-1", new Observador("autor-1", false, true, false))).thenReturn(List.of(entrada()));

        mvc.perform(como(get("/api/history/questions/q-1"), "autor-1", "AUTHOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("PREGUNTA_ENVIADA"))
                .andExpect(jsonPath("$[0].resumen").value("autor-1 envió a revisión «Observer» (versión 1)"))
                .andExpect(jsonPath("$[0].detalles.version").value(1))
                .andExpect(jsonPath("$[0].actorId").value("autor-1"));
    }

    @Test
    @DisplayName("el observador refleja los roles del usuario")
    void observador() throws Exception {
        when(servicio.lineaDeTiempo("q-1", new Observador("u-1", true, false, true))).thenReturn(List.of());

        mvc.perform(como(get("/api/history/questions/q-1"), "u-1", "ADMIN,REVIEWER")).andExpect(status().isOk());

        verify(servicio).lineaDeTiempo("q-1", new Observador("u-1", true, false, true));
    }

    @Test
    @DisplayName("una pregunta sin historial o ajena responde 404")
    void noEncontrada() throws Exception {
        when(servicio.lineaDeTiempo("q-9", new Observador("autor-1", false, true, false)))
                .thenThrow(new HistorialNoEncontradoException("q-9"));

        mvc.perform(como(get("/api/history/questions/q-9"), "autor-1", "AUTHOR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("HISTORIAL_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("sin identidad responde 401 y con otro rol, 403")
    void seguridad() throws Exception {
        mvc.perform(get("/api/history/me")).andExpect(status().isUnauthorized());
        mvc.perform(como(get("/api/history/me"), "est-1", "STUDENT")).andExpect(status().isForbidden());
        mvc.perform(como(get("/api/history/questions/q-1"), "est-1", "STUDENT")).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("GET /me devuelve lo que puedo ver, con paginación desde 0")
    void visibles() throws Exception {
        when(servicio.visiblesPara(new Observador("autor-1", false, true, false), 1, 20))
                .thenReturn(new Pagina<>(List.of(entrada()), 1, 20, 1));

        mvc.perform(como(get("/api/history/me"), "autor-1", "AUTHOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value("h-1"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("valida la paginación")
    void paginacion() throws Exception {
        mvc.perform(como(get("/api/history/me?page=-1"), "autor-1", "AUTHOR")).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/history/me?size=0"), "autor-1", "AUTHOR")).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/history/audit?size=101"), "admin-1", "ADMIN")).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("GET /audit aplica los filtros y es solo del administrador")
    void auditoria() throws Exception {
        FiltroAuditoria filtro = new FiltroAuditoria(TipoEntrada.INICIO_DE_SESION_FALLIDO, "intruso", null,
                Instant.parse("2026-10-08T00:00:00Z"), Instant.parse("2026-10-09T00:00:00Z"));
        when(servicio.auditoria(filtro, 2, 5)).thenReturn(new Pagina<>(List.of(entrada()), 2, 5, 6));

        mvc.perform(como(get("/api/history/audit?tipo=INICIO_DE_SESION_FALLIDO&actorId=intruso"
                        + "&desde=2026-10-08T00:00:00Z&hasta=2026-10-09T00:00:00Z&page=1&size=5"), "admin-1", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value("h-1"))
                .andExpect(jsonPath("$.pagina").value(1));

        mvc.perform(como(get("/api/history/audit"), "autor-1", "AUTHOR")).andExpect(status().isForbidden());
        mvc.perform(como(get("/api/history/audit"), "rev-1", "REVIEWER")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un tipo o una fecha inválidos responden 400; un rango invertido también")
    void filtrosInvalidos() throws Exception {
        mvc.perform(como(get("/api/history/audit?tipo=NADA"), "admin-1", "ADMIN")).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/history/audit?desde=ayer"), "admin-1", "ADMIN")).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/history/audit?desde=2026-10-09T00:00:00Z&hasta=2026-10-08T00:00:00Z"), "admin-1", "ADMIN"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }
}
