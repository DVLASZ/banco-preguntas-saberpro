package co.unicauca.bancopreguntas.review.api;

import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import co.unicauca.bancopreguntas.review.application.ReviewApplicationService;
import co.unicauca.bancopreguntas.review.domain.AccesoNoPermitidoException;
import co.unicauca.bancopreguntas.review.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.review.domain.CasoDeRevision;
import co.unicauca.bancopreguntas.review.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.review.domain.CasoPendiente;
import co.unicauca.bancopreguntas.review.domain.Decision;
import co.unicauca.bancopreguntas.review.domain.EstadoRonda;
import co.unicauca.bancopreguntas.review.domain.Evaluacion;
import co.unicauca.bancopreguntas.review.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.review.domain.Observador;
import co.unicauca.bancopreguntas.review.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.review.domain.Pagina;
import co.unicauca.bancopreguntas.review.domain.ResultadoDeDecision;
import co.unicauca.bancopreguntas.review.domain.ResultadoDeEvaluacion;
import co.unicauca.bancopreguntas.review.domain.RevisionInvalidaException;
import co.unicauca.bancopreguntas.review.domain.VistaDelCaso;
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

@WebMvcTest(ReviewController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("ReviewController (API REST)")
class ReviewControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReviewApplicationService servicio;

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

    private static CasoDeRevision casoConRevisores() {
        CasoDeRevision caso = CasoDeRevision.nuevo("c-1", "Q-1", "autor-1", "Observer", "INGLES", T0);
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
    @DisplayName("POST /{id}/assignments asigna revisores y devuelve la revisión completa")
    void asignar() throws Exception {
        when(servicio.asignarRevisores("Q-1", List.of("rev-1", "rev-2"), "admin-1")).thenReturn(casoConRevisores());

        mvc.perform(comoAdmin(post("/api/reviews/Q-1/assignments"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewerIds\":[\"rev-1\",\"rev-2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preguntaId").value("Q-1"))
                .andExpect(jsonPath("$.rondas[0].estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.rondas[0].revisores[0].revisorId").value("rev-1"))
                .andExpect(jsonPath("$.rondas[0].revisores[0].asignadoPor").value("admin-1"))
                .andExpect(jsonPath("$.rondas[0].revisores[0].evaluacion").doesNotExist());
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

    // ---------------------------------------------------------------- consultar una revisión

    private static VistaDelCaso vista() {
        return new VistaDelCaso("Q-1", "autor-1", "Observer", "INGLES", List.of(
                new VistaDelCaso.VistaDeRonda(1, 1, EstadoRonda.RECHAZADA, T0, T0, null, 2, 1, true, List.of(
                        new VistaDelCaso.VistaDeRevisor("rev-1", "admin-1", T0, false,
                                new Evaluacion("rev-1", Decision.RECHAZAR, "Mal", T0, false)),
                        new VistaDelCaso.VistaDeRevisor("rev-2", "admin-1", T0, false, null)))));
    }

    @Test
    @DisplayName("GET /{id} entrega la revisión con las evaluaciones")
    void consultar() throws Exception {
        when(servicio.caso("Q-1", new Observador("rev-1", false))).thenReturn(vista());

        mvc.perform(comoRevisor(get("/api/reviews/Q-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rondas[0].estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.rondas[0].detalleVisible").value(true))
                .andExpect(jsonPath("$.rondas[0].revisores[0].evaluacion.decision").value("RECHAZAR"))
                .andExpect(jsonPath("$.rondas[0].revisores[0].evaluacion.observacion").value("Mal"))
                .andExpect(jsonPath("$.rondas[0].revisores[1].evaluacion").doesNotExist());
    }

    @Test
    @DisplayName("el administrador consulta como administrador y el autor como autor")
    void consultarSegunElRol() throws Exception {
        when(servicio.caso(eq("Q-1"), any())).thenReturn(vista());

        mvc.perform(comoAdmin(get("/api/reviews/Q-1"))).andExpect(status().isOk());
        mvc.perform(comoAutor(get("/api/reviews/Q-1"))).andExpect(status().isOk());

        verify(servicio).caso("Q-1", new Observador("admin-1", true));
        verify(servicio).caso("Q-1", new Observador("autor-1", false));
    }

    @Test
    @DisplayName("quien no participa recibe 403 y una pregunta desconocida 404")
    void consultarErrores() throws Exception {
        when(servicio.caso(eq("Q-2"), any())).thenThrow(new AccesoNoPermitidoException("No participas"));
        when(servicio.caso(eq("Q-9"), any())).thenThrow(new CasoNoEncontradoException("Q-9"));

        mvc.perform(comoRevisor(get("/api/reviews/Q-2"))).andExpect(status().isForbidden());
        mvc.perform(comoRevisor(get("/api/reviews/Q-9"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("un usuario con otro rol no puede consultar revisiones")
    void consultarOtroRol() throws Exception {
        mvc.perform(como(get("/api/reviews/Q-1"), "est-1", "STUDENT")).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    // ---------------------------------------------------------------- evaluar

    @Test
    @DisplayName("POST /{id}/evaluations envía la evaluación e informa si decidió la ronda")
    void evaluar() throws Exception {
        CasoDeRevision caso = casoConRevisores();
        ResultadoDeEvaluacion resultado = caso.evaluar("rev-1", Decision.RECHAZAR, "Mal",
                new co.unicauca.bancopreguntas.review.domain.Unanimidad(), T0);
        when(servicio.evaluar("Q-1", "rev-1", Decision.RECHAZAR, "Mal")).thenReturn(resultado);

        mvc.perform(comoRevisor(post("/api/reviews/Q-1/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"RECHAZAR\",\"observacion\":\"Mal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoDeLaRonda").value(ResultadoDeDecision.RECHAZADA.name()))
                .andExpect(jsonPath("$.tardia").value(false))
                .andExpect(jsonPath("$.ronda").value(1));
    }

    @Test
    @DisplayName("evaluar sin decisión o con una desconocida responde 400")
    void evaluarDecisionInvalida() throws Exception {
        mvc.perform(comoRevisor(post("/api/reviews/Q-1/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacion\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(comoRevisor(post("/api/reviews/Q-1/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"TAL_VEZ\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("la observación no puede superar 4000 caracteres")
    void observacionDemasiadoLarga() throws Exception {
        mvc.perform(comoRevisor(post("/api/reviews/Q-1/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APROBAR\",\"observacion\":\"" + "x".repeat(4001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("evaluar exige el rol REVIEWER")
    void evaluarSoloRevisor() throws Exception {
        mvc.perform(comoAdmin(post("/api/reviews/Q-1/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBAR\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(comoAutor(post("/api/reviews/Q-1/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBAR\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("evaluar sin estar asignado responde 403; evaluar dos veces, 409; rechazar sin observación, 400")
    void evaluarErrores() throws Exception {
        when(servicio.evaluar("Q-1", "rev-1", Decision.APROBAR, null))
                .thenThrow(new AccesoNoPermitidoException("No estás asignado"))
                .thenThrow(new OperacionNoPermitidaException("Ya evaluaste esta pregunta"));
        when(servicio.evaluar("Q-1", "rev-1", Decision.RECHAZAR, null))
                .thenThrow(new RevisionInvalidaException("Debes escribir una observación"));

        String aprobar = "{\"decision\":\"APROBAR\"}";
        mvc.perform(comoRevisor(post("/api/reviews/Q-1/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON).content(aprobar))
                .andExpect(status().isForbidden());
        mvc.perform(comoRevisor(post("/api/reviews/Q-1/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON).content(aprobar))
                .andExpect(status().isConflict());
        mvc.perform(comoRevisor(post("/api/reviews/Q-1/evaluations"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"RECHAZAR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REVISION_INVALIDA"));
    }

    @Test
    @DisplayName("una ruta que no existe responde 404, no 500")
    void rutaDesconocida() throws Exception {
        mvc.perform(comoAdmin(get("/api/reviews/Q-1/nada/mas/aqui"))).andExpect(status().isNotFound());
    }
}
