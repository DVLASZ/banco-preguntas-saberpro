package co.unicauca.bancopreguntas.assignment.api;

import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import co.unicauca.bancopreguntas.assignment.application.AssignmentApplicationService;
import co.unicauca.bancopreguntas.assignment.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.assignment.domain.CasoDeAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.assignment.domain.CasoPendiente;
import co.unicauca.bancopreguntas.assignment.domain.EstadoRonda;
import co.unicauca.bancopreguntas.assignment.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.assignment.domain.Pagina;
import co.unicauca.bancopreguntas.assignment.domain.RevisionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssignmentController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("AssignmentController (API REST)")
class AssignmentControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AssignmentApplicationService servicio;

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder peticion, String usuario, String roles) {
        return peticion.header("X-User-Id", usuario).header("X-User-Roles", roles);
    }

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder p) {
        return como(p, "admin-1", "ADMIN");
    }

    private static MockHttpServletRequestBuilder comoRevisor(MockHttpServletRequestBuilder p) {
        return como(p, "rev-1", "REVIEWER");
    }

    private static MockHttpServletRequestBuilder comoAutor(MockHttpServletRequestBuilder p) {
        return como(p, "autor-1", "AUTHOR");
    }

    private static CasoDeAsignacion casoConRevisores() {
        CasoDeAsignacion caso = CasoDeAsignacion.nuevo("c-1", "Q-1", "autor-1", "Observer", "INGLES", T0);
        caso.iniciarRonda(1, T0);
        caso.asignarRevisores(List.of("rev-1", "rev-2"), "admin-1", 5, T0);
        return caso;
    }

    // ---------------------------------------------------------------- configuración

    @Test
    @DisplayName("GET /config informa el mínimo (1) y el máximo configurado")
    void configuracion() throws Exception {
        when(servicio.maximoDeRevisores()).thenReturn(5);

        mvc.perform(comoAutor(get("/api/reviews/config")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimoDeRevisores").value(1))
                .andExpect(jsonPath("$.maximoDeRevisores").value(5));
    }

    @Test
    @DisplayName("sin identidad responde 401")
    void sinUsuario() throws Exception {
        mvc.perform(get("/api/reviews/config"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    // ---------------------------------------------------------------- pendientes de asignación

    @Test
    @DisplayName("GET /pending-assignment lista las preguntas por asignar (ADMIN) con paginación desde 0")
    void pendientes() throws Exception {
        when(servicio.pendientesDeAsignacion(1, 10)).thenReturn(new Pagina<>(
                List.of(new CasoPendiente("Q-1", "Observer", "INGLES", "autor-1", 1, T0)), 1, 10, 1));

        mvc.perform(comoAdmin(get("/api/reviews/pending-assignment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].preguntaId").value("Q-1"))
                .andExpect(jsonPath("$.contenido[0].titulo").value("Observer"))
                .andExpect(jsonPath("$.contenido[0].ronda").value(1))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    @DisplayName("solo el administrador ve las preguntas por asignar")
    void pendientesSoloAdmin() throws Exception {
        mvc.perform(comoRevisor(get("/api/reviews/pending-assignment"))).andExpect(status().isForbidden());
        mvc.perform(comoAutor(get("/api/reviews/pending-assignment"))).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("valida los parámetros de paginación")
    void paginacionInvalida() throws Exception {
        mvc.perform(comoAdmin(get("/api/reviews/pending-assignment?page=-1"))).andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(get("/api/reviews/pending-assignment?size=0"))).andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(get("/api/reviews/pending-assignment?size=101"))).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    // ---------------------------------------------------------------- asignar

    @Test
    @DisplayName("POST /{id}/assignments asigna revisores y devuelve las rondas con sus revisores")
    void asignar() throws Exception {
        when(servicio.asignarRevisores("Q-1", List.of("rev-1", "rev-2"), "admin-1")).thenReturn(casoConRevisores());

        mvc.perform(comoAdmin(post("/api/reviews/Q-1/assignments"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewerIds\":[\"rev-1\",\"rev-2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preguntaId").value("Q-1"))
                .andExpect(jsonPath("$.rondas[0].estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.rondas[0].revisores[0].revisorId").value("rev-1"))
                .andExpect(jsonPath("$.rondas[0].revisores[0].asignadoPor").value("admin-1"))
                .andExpect(jsonPath("$.rondas[0].revisores[0].decision").doesNotExist());
    }

    @Test
    @DisplayName("asignar exige el rol ADMIN")
    void asignarSoloAdmin() throws Exception {
        mvc.perform(comoRevisor(post("/api/reviews/Q-1/assignments"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewerIds\":[\"rev-1\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("asignar con la lista vacía o sin ella responde 400")
    void asignarListaVacia() throws Exception {
        mvc.perform(comoAdmin(post("/api/reviews/Q-1/assignments"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewerIds\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(post("/api/reviews/Q-1/assignments"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("una asignación inválida responde 400 con el motivo")
    void asignarInvalido() throws Exception {
        when(servicio.asignarRevisores(eq("Q-1"), any(), any()))
                .thenThrow(new RevisionInvalidaException("El autor de la pregunta no puede ser su propio revisor"));

        mvc.perform(comoAdmin(post("/api/reviews/Q-1/assignments"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewerIds\":[\"autor-1\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REVISION_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").value("El autor de la pregunta no puede ser su propio revisor"));
    }

    @Test
    @DisplayName("asignar a una pregunta sin revisión responde 404")
    void asignarNoEncontrada() throws Exception {
        when(servicio.asignarRevisores(eq("Q-9"), any(), any())).thenThrow(new CasoNoEncontradoException("Q-9"));

        mvc.perform(comoAdmin(post("/api/reviews/Q-9/assignments"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewerIds\":[\"rev-1\"]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REVISION_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("asignar a una ronda ya decidida responde 409")
    void asignarConflicto() throws Exception {
        when(servicio.asignarRevisores(eq("Q-1"), any(), any()))
                .thenThrow(new OperacionNoPermitidaException("La ronda está RECHAZADA"));

        mvc.perform(comoAdmin(post("/api/reviews/Q-1/assignments"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewerIds\":[\"rev-3\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }

    // ---------------------------------------------------------------- bandeja del revisor

    @Test
    @DisplayName("GET /assigned devuelve la bandeja del revisor autenticado, por defecto las pendientes")
    void bandeja() throws Exception {
        when(servicio.asignadasA("rev-1", FiltroAsignacion.PENDIENTES, 1, 10)).thenReturn(new Pagina<>(
                List.of(new AsignacionDeRevisor("Q-1", "Observer", "INGLES", "autor-1", 1, T0,
                        EstadoRonda.EN_CURSO, null)), 1, 10, 1));

        mvc.perform(comoRevisor(get("/api/reviews/assigned")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].preguntaId").value("Q-1"))
                .andExpect(jsonPath("$.contenido[0].estadoRonda").value("EN_CURSO"))
                .andExpect(jsonPath("$.contenido[0].pendiente").value(true))
                .andExpect(jsonPath("$.contenido[0].miDecision").doesNotExist());
    }

    @Test
    @DisplayName("la bandeja acepta el filtro de estado")
    void bandejaConFiltro() throws Exception {
        when(servicio.asignadasA("rev-1", FiltroAsignacion.EVALUADAS, 3, 5))
                .thenReturn(new Pagina<>(List.of(), 3, 5, 0));

        mvc.perform(comoRevisor(get("/api/reviews/assigned?estado=EVALUADAS&page=2&size=5")))
                .andExpect(status().isOk());
        verify(servicio).asignadasA("rev-1", FiltroAsignacion.EVALUADAS, 3, 5);
    }

    @Test
    @DisplayName("un filtro desconocido responde 400")
    void bandejaFiltroInvalido() throws Exception {
        mvc.perform(comoRevisor(get("/api/reviews/assigned?estado=QUIZAS"))).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("la bandeja exige el rol REVIEWER")
    void bandejaSoloRevisor() throws Exception {
        mvc.perform(comoAdmin(get("/api/reviews/assigned"))).andExpect(status().isForbidden());
        mvc.perform(comoAutor(get("/api/reviews/assigned"))).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("una ruta que no existe responde 404, no 500")
    void rutaDesconocida() throws Exception {
        mvc.perform(comoAdmin(get("/api/reviews/Q-1/nada/mas/aqui"))).andExpect(status().isNotFound());
    }
}
