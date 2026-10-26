package co.unicauca.bancopreguntas.audit.api;

import co.unicauca.bancopreguntas.audit.application.AuditApplicationService;
import co.unicauca.bancopreguntas.audit.domain.EntradaDeAuditoria;
import co.unicauca.bancopreguntas.audit.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.audit.domain.Pagina;
import co.unicauca.bancopreguntas.audit.domain.TipoEntrada;
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

@WebMvcTest(AuditController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("AuditController (API REST)")
class AuditControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuditApplicationService servicio;

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder p) {
        return p.header("X-User-Id", "admin-1").header("X-User-Roles", "ADMIN");
    }

    private static EntradaDeAuditoria entrada() {
        return new EntradaDeAuditoria("a-1", "e-1", null, "intruso", TipoEntrada.INICIO_DE_SESION_FALLIDO,
                "Intento fallido de inicio de sesión de intruso", "{\"ip\":\"10.0.0.1\"}", T0, T0);
    }

    @Test
    @DisplayName("GET /api/audit aplica los filtros, devuelve el detalle como JSON y pagina desde 0")
    void consulta() throws Exception {
        FiltroAuditoria filtro = new FiltroAuditoria(TipoEntrada.INICIO_DE_SESION_FALLIDO, "intruso", null,
                Instant.parse("2026-10-08T00:00:00Z"), null);
        when(servicio.consultar(filtro, 2, 5)).thenReturn(new Pagina<>(List.of(entrada()), 2, 5, 6));

        mvc.perform(comoAdmin(get("/api/audit?tipo=INICIO_DE_SESION_FALLIDO&actorId=intruso&desde=2026-10-08T00:00:00Z&page=1&size=5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].tipo").value("INICIO_DE_SESION_FALLIDO"))
                .andExpect(jsonPath("$.contenido[0].actorId").value("intruso"))
                .andExpect(jsonPath("$.contenido[0].detalles.ip").value("10.0.0.1"))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.totalElementos").value(6));
    }

    @Test
    @DisplayName("solo el administrador consulta la auditoría; sin identidad responde 401")
    void seguridad() throws Exception {
        mvc.perform(get("/api/audit").header("X-User-Id", "autor-1").header("X-User-Roles", "AUTHOR")).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").header("X-User-Id", "rev-1").header("X-User-Roles", "REVIEWER")).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit")).andExpect(status().isUnauthorized());

        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("un tipo, una fecha o un tamaño inválidos responden 400; un rango invertido también")
    void invalidos() throws Exception {
        mvc.perform(comoAdmin(get("/api/audit?tipo=NADA"))).andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(get("/api/audit?desde=ayer"))).andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(get("/api/audit?size=101"))).andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(get("/api/audit?desde=2026-10-09T00:00:00Z&hasta=2026-10-08T00:00:00Z"))).andExpect(status().isBadRequest());
    }
}
