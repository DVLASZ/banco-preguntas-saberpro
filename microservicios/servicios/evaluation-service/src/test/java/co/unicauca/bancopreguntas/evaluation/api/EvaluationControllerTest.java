package co.unicauca.bancopreguntas.evaluation.api;

import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import co.unicauca.bancopreguntas.evaluation.application.EvaluationApplicationService;
import co.unicauca.bancopreguntas.evaluation.domain.AccesoNoPermitidoException;
import co.unicauca.bancopreguntas.evaluation.domain.CasoDeEvaluacion;
import co.unicauca.bancopreguntas.evaluation.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.evaluation.domain.Decision;
import co.unicauca.bancopreguntas.evaluation.domain.EstadoRonda;
import co.unicauca.bancopreguntas.evaluation.domain.Evaluacion;
import co.unicauca.bancopreguntas.evaluation.domain.Observador;
import co.unicauca.bancopreguntas.evaluation.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.evaluation.domain.ResultadoDeDecision;
import co.unicauca.bancopreguntas.evaluation.domain.ResultadoDeEvaluacion;
import co.unicauca.bancopreguntas.evaluation.domain.RevisionInvalidaException;
import co.unicauca.bancopreguntas.evaluation.domain.VistaDelCaso;
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

@WebMvcTest(EvaluationController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("EvaluationController (API REST)")
class EvaluationControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EvaluationApplicationService servicio;

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

    private static CasoDeEvaluacion casoConRevisores() {
        CasoDeEvaluacion caso = CasoDeEvaluacion.nuevo("c-1", "Q-1", "autor-1", "Observer", "INGLES", T0);
        caso.iniciarRonda(1, T0);
        caso.registrarAsignaciones(1, List.of("rev-1", "rev-2"), "admin-1", false, T0);
        return caso;
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
        CasoDeEvaluacion caso = casoConRevisores();
        ResultadoDeEvaluacion resultado = caso.evaluar("rev-1", Decision.RECHAZAR, "Mal",
                new co.unicauca.bancopreguntas.evaluation.domain.Unanimidad(), T0);
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
