package co.unicauca.bancopreguntas.tracking.api;

import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import co.unicauca.bancopreguntas.tracking.application.TrackingApplicationService;
import co.unicauca.bancopreguntas.tracking.domain.AnalisisDeCompetencias;
import co.unicauca.bancopreguntas.tracking.domain.EstadisticasIndividuales;
import co.unicauca.bancopreguntas.tracking.domain.Pagina;
import co.unicauca.bancopreguntas.tracking.domain.ReporteDeGrupo;
import co.unicauca.bancopreguntas.tracking.domain.Resultados;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoRegistrado;
import co.unicauca.bancopreguntas.tracking.domain.Umbrales;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static co.unicauca.bancopreguntas.tracking.domain.Resultados.comp;
import static co.unicauca.bancopreguntas.tracking.domain.Resultados.resultado;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackingController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("TrackingController (API REST)")
class TrackingControllerTest {

    private static final Umbrales UMBRALES = new Umbrales(70, 50, 5);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TrackingApplicationService servicio;

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder p, String usuario, String roles) {
        return p.header("X-User-Id", usuario).header("X-User-Roles", roles);
    }

    private static MockHttpServletRequestBuilder comoEstudiante(MockHttpServletRequestBuilder p) {
        return como(p, "ana", "STUDENT");
    }

    private static MockHttpServletRequestBuilder comoDocente(MockHttpServletRequestBuilder p) {
        return como(p, "docente1", "TEACHER");
    }

    private static List<ResultadoRegistrado> datos() {
        return List.of(resultado("s-1", "ana", 1, 4, 10, comp("INGLES", 4, 10)), resultado("s-2", "ana", 2, 9, 10, comp("INGLES", 9, 10)));
    }

    @Test
    @DisplayName("GET /me/history devuelve mi historial paginado desde 0")
    void historial() throws Exception {
        when(servicio.historial("ana", 1, 10)).thenReturn(new Pagina<>(datos(), 1, 10, 2));

        mvc.perform(comoEstudiante(get("/api/tracking/me/history")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].simulacroId").value("s-1"))
                .andExpect(jsonPath("$.contenido[0].puntaje").value(40.0))
                .andExpect(jsonPath("$.contenido[0].porCompetencia[0].competencia").value("INGLES"))
                .andExpect(jsonPath("$.contenido[0].porCompetencia[0].porcentaje").value(40.0))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    @DisplayName("valida la paginación")
    void paginacion() throws Exception {
        mvc.perform(comoEstudiante(get("/api/tracking/me/history?page=-1"))).andExpect(status().isBadRequest());
        mvc.perform(comoEstudiante(get("/api/tracking/me/history?size=101"))).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("GET /me/stats devuelve mis estadísticas")
    void estadisticas() throws Exception {
        when(servicio.estadisticas("ana")).thenReturn(EstadisticasIndividuales.de("ana", datos()));

        mvc.perform(comoEstudiante(get("/api/tracking/me/stats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudianteId").value("ana"))
                .andExpect(jsonPath("$.simulacros").value(2))
                .andExpect(jsonPath("$.puntajePromedio").value(65.0))
                .andExpect(jsonPath("$.mejorPuntaje").value(90.0))
                .andExpect(jsonPath("$.ultimoPuntaje").value(90.0))
                .andExpect(jsonPath("$.tendencia").value("MEJORANDO"));
    }

    @Test
    @DisplayName("un estudiante sin simulacros recibe ceros y no un error; no trae último puntaje")
    void sinSimulacros() throws Exception {
        when(servicio.estadisticas("ana")).thenReturn(EstadisticasIndividuales.de("ana", List.of()));

        mvc.perform(comoEstudiante(get("/api/tracking/me/stats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulacros").value(0))
                .andExpect(jsonPath("$.tendencia").value("SIN_DATOS"))
                .andExpect(jsonPath("$.ultimoPuntaje").doesNotExist());
    }

    @Test
    @DisplayName("GET /me/competencies devuelve fortalezas, debilidades y los umbrales usados")
    void competencias() throws Exception {
        when(servicio.competencias("ana")).thenReturn(AnalisisDeCompetencias.de("ana", List.of(
                resultado("s-1", "ana", 1, 6, 12, comp("LECTURA_CRITICA", 5, 6), comp("INGLES", 1, 6))), UMBRALES));
        when(servicio.umbrales()).thenReturn(UMBRALES);

        mvc.perform(comoEstudiante(get("/api/tracking/me/competencies")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.umbralFortaleza").value(70.0))
                .andExpect(jsonPath("$.umbralDebilidad").value(50.0))
                .andExpect(jsonPath("$.fortalezas[0].competencia").value("LECTURA_CRITICA"))
                .andExpect(jsonPath("$.debilidades[0].competencia").value("INGLES"));
    }

    @Test
    @DisplayName("lo propio es solo del estudiante")
    void soloEstudiante() throws Exception {
        mvc.perform(comoDocente(get("/api/tracking/me/stats"))).andExpect(status().isForbidden());
        mvc.perform(como(get("/api/tracking/me/history"), "autor1", "AUTHOR")).andExpect(status().isForbidden());
        mvc.perform(get("/api/tracking/me/competencies")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("docentes y administradores consultan a cualquier estudiante; los estudiantes no")
    void consultaDeOtro() throws Exception {
        when(servicio.estadisticas("luis")).thenReturn(EstadisticasIndividuales.de("luis", List.of()));
        when(servicio.historial("luis", 1, 10)).thenReturn(new Pagina<>(List.of(), 1, 10, 0));
        when(servicio.competencias("luis")).thenReturn(AnalisisDeCompetencias.de("luis", List.of(), UMBRALES));
        when(servicio.umbrales()).thenReturn(UMBRALES);

        mvc.perform(comoDocente(get("/api/tracking/students/luis/stats"))).andExpect(status().isOk());
        mvc.perform(como(get("/api/tracking/students/luis/history"), "admin", "ADMIN")).andExpect(status().isOk());
        mvc.perform(comoDocente(get("/api/tracking/students/luis/competencies"))).andExpect(status().isOk());
        mvc.perform(comoEstudiante(get("/api/tracking/students/luis/stats"))).andExpect(status().isForbidden());
        verify(servicio).estadisticas("luis");
    }

    @Test
    @DisplayName("GET /reports/group devuelve el reporte del grupo a docentes y administradores")
    void reporte() throws Exception {
        when(servicio.reporteDeGrupo()).thenReturn(ReporteDeGrupo.de(datos(), UMBRALES, Resultados.T0));

        mvc.perform(comoDocente(get("/api/tracking/reports/group")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiantes").value(1))
                .andExpect(jsonPath("$.simulacros").value(2))
                .andExpect(jsonPath("$.promedioGeneral").value(65.0))
                .andExpect(jsonPath("$.estudiantesDetalle[0].estudianteId").value("ana"))
                .andExpect(jsonPath("$.porCompetencia[0].competencia").value("INGLES"));
        mvc.perform(comoEstudiante(get("/api/tracking/reports/group"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /reports/group.csv entrega el detalle como archivo CSV")
    void csv() throws Exception {
        when(servicio.reporteDeGrupo()).thenReturn(ReporteDeGrupo.de(datos(), UMBRALES, Resultados.T0));

        mvc.perform(comoDocente(get("/api/tracking/reports/group.csv")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"reporte-de-grupo.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("estudiante,simulacros,promedio")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ana,2,65.00,90.00,90.00,MEJORANDO,")));
        mvc.perform(comoEstudiante(get("/api/tracking/reports/group.csv"))).andExpect(status().isForbidden());
    }
}
