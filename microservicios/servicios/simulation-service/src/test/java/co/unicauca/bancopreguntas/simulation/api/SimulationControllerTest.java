package co.unicauca.bancopreguntas.simulation.api;

import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import co.unicauca.bancopreguntas.simulation.application.SimulationApplicationService;
import co.unicauca.bancopreguntas.simulation.application.VistaDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.CriteriosDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.Pagina;
import co.unicauca.bancopreguntas.simulation.domain.Preguntas;
import co.unicauca.bancopreguntas.simulation.domain.PreguntasInsuficientesException;
import co.unicauca.bancopreguntas.simulation.domain.ResumenDelCatalogo;
import co.unicauca.bancopreguntas.simulation.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.simulation.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.simulation.domain.Simulacro;
import co.unicauca.bancopreguntas.simulation.domain.SimulacroNoEncontradoException;
import co.unicauca.bancopreguntas.simulation.domain.TiempoAgotadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulationController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("SimulationController (API REST)")
class SimulationControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-16T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SimulationApplicationService servicio;

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder p, String usuario, String roles) {
        return p.header("X-User-Id", usuario).header("X-User-Roles", roles);
    }

    private static MockHttpServletRequestBuilder comoEstudiante(MockHttpServletRequestBuilder p) {
        return como(p, "estudiante1", "STUDENT");
    }

    private static Simulacro simulacro() {
        return Simulacro.iniciar("s-1", "estudiante1", Preguntas.criterios(2, 30), Preguntas.lectura(2), T0);
    }

    private static final String CREAR = "{\"competencias\":[\"LECTURA_CRITICA\"],\"cantidad\":2,\"duracionMinutos\":30}";

    @Test
    @DisplayName("POST / genera el simulacro con 201 y no revela la respuesta correcta")
    void crear() throws Exception {
        when(servicio.iniciar(anyString(), any(CriteriosDeSimulacro.class))).thenReturn(new VistaDeSimulacro(simulacro(), 1800));

        mvc.perform(comoEstudiante(post("/api/simulations")).contentType(MediaType.APPLICATION_JSON).content(CREAR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("s-1"))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.segundosRestantes").value(1800))
                .andExpect(jsonPath("$.totalPreguntas").value(2))
                .andExpect(jsonPath("$.preguntas[0].enunciado").exists())
                .andExpect(jsonPath("$.preguntas[0].respuestaCorrecta").doesNotExist())
                .andExpect(jsonPath("$.preguntas[0].acertada").doesNotExist())
                .andExpect(jsonPath("$.resultado").doesNotExist());

        ArgumentCaptor<CriteriosDeSimulacro> criterios = ArgumentCaptor.forClass(CriteriosDeSimulacro.class);
        verify(servicio).iniciar(eq("estudiante1"), criterios.capture());
        assertThat(criterios.getValue().competencias()).containsExactly("LECTURA_CRITICA");
        assertThat(criterios.getValue().cantidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST / valida la cantidad y la duración")
    void crearInvalido() throws Exception {
        mvc.perform(comoEstudiante(post("/api/simulations")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":0,\"duracionMinutos\":30}")).andExpect(status().isBadRequest());
        mvc.perform(comoEstudiante(post("/api/simulations")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":5,\"duracionMinutos\":241}")).andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("solo el estudiante genera simulacros")
    void soloEstudiante() throws Exception {
        mvc.perform(como(post("/api/simulations"), "docente1", "TEACHER").contentType(MediaType.APPLICATION_JSON).content(CREAR))
                .andExpect(status().isForbidden());
        mvc.perform(como(post("/api/simulations"), "autor1", "AUTHOR").contentType(MediaType.APPLICATION_JSON).content(CREAR))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/simulations").contentType(MediaType.APPLICATION_JSON).content(CREAR)).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("si faltan preguntas responde 409 con el motivo")
    void insuficientes() throws Exception {
        when(servicio.iniciar(anyString(), any())).thenThrow(new PreguntasInsuficientesException(1, 5));

        mvc.perform(comoEstudiante(post("/api/simulations")).contentType(MediaType.APPLICATION_JSON).content(CREAR))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PREGUNTAS_INSUFICIENTES"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Solo hay 1")));
    }

    @Test
    @DisplayName("GET / lista mis simulacros con paginación desde 0")
    void mios() throws Exception {
        when(servicio.misSimulacros("estudiante1", 1, 10)).thenReturn(new Pagina<>(List.of(simulacro()), 1, 10, 1));

        mvc.perform(comoEstudiante(get("/api/simulations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value("s-1"))
                .andExpect(jsonPath("$.contenido[0].totalPreguntas").value(2))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(comoEstudiante(get("/api/simulations?size=0"))).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/simulations"), "docente1", "TEACHER")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /{id} lo consulta el estudiante, y docentes y administradores ven los de cualquiera")
    void obtener() throws Exception {
        when(servicio.obtener("s-1", "estudiante1", false)).thenReturn(new VistaDeSimulacro(simulacro(), 900));
        when(servicio.obtener("s-1", "docente1", true)).thenReturn(new VistaDeSimulacro(simulacro(), 900));

        mvc.perform(comoEstudiante(get("/api/simulations/s-1"))).andExpect(status().isOk()).andExpect(jsonPath("$.segundosRestantes").value(900));
        mvc.perform(como(get("/api/simulations/s-1"), "docente1", "TEACHER")).andExpect(status().isOk());
        mvc.perform(como(get("/api/simulations/s-1"), "autor1", "AUTHOR")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("el simulacro de otro estudiante responde 403 y uno inexistente 404")
    void obtenerAjenoOInexistente() throws Exception {
        when(servicio.obtener("s-2", "estudiante1", false)).thenThrow(new AccesoDenegadoException("Este simulacro es de otro estudiante"));
        when(servicio.obtener("s-9", "estudiante1", false)).thenThrow(new SimulacroNoEncontradoException("s-9"));

        mvc.perform(comoEstudiante(get("/api/simulations/s-2"))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(comoEstudiante(get("/api/simulations/s-9"))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SIMULACRO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("POST /{id}/answers registra la respuesta; valida la opción")
    void responder() throws Exception {
        when(servicio.responder("s-1", "estudiante1", "q-1", 'B')).thenReturn(new VistaDeSimulacro(simulacro(), 800));

        mvc.perform(comoEstudiante(post("/api/simulations/s-1/answers")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preguntaId\":\"q-1\",\"opcion\":\"b\"}").characterEncoding("UTF-8"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.segundosRestantes").value(800));
        mvc.perform(comoEstudiante(post("/api/simulations/s-1/answers")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":\"q-1\",\"opcion\":\"E\"}")).andExpect(status().isBadRequest());
        mvc.perform(comoEstudiante(post("/api/simulations/s-1/answers")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"opcion\":\"A\"}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("responder con el tiempo agotado responde 409 TIEMPO_AGOTADO")
    void tiempoAgotado() throws Exception {
        when(servicio.responder(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyChar())).thenThrow(new TiempoAgotadoException());

        mvc.perform(comoEstudiante(post("/api/simulations/s-1/answers")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preguntaId\":\"q-1\",\"opcion\":\"A\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TIEMPO_AGOTADO"));
    }

    @Test
    @DisplayName("POST /{id}/finish califica y revela las respuestas; dos veces responde 409")
    void finalizar() throws Exception {
        Simulacro terminado = simulacro();
        terminado.responder("q-1", 'B', T0.plusSeconds(5));
        terminado.finalizar(T0.plusSeconds(60));
        when(servicio.finalizar("s-1", "estudiante1")).thenReturn(new VistaDeSimulacro(terminado, 0));
        when(servicio.finalizar("s-2", "estudiante1")).thenThrow(new OperacionNoPermitidaException("El simulacro ya terminó"));

        mvc.perform(comoEstudiante(post("/api/simulations/s-1/finish")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.resultado.correctas").value(1))
                .andExpect(jsonPath("$.resultado.puntaje").value(50.0))
                .andExpect(jsonPath("$.resultado.porCompetencia[0].competencia").value("LECTURA_CRITICA"))
                .andExpect(jsonPath("$.resultado.porCompetencia[0].porcentaje").value(50.0))
                .andExpect(jsonPath("$.preguntas[0].respuestaCorrecta").value("B"))
                .andExpect(jsonPath("$.preguntas[0].acertada").value(true))
                .andExpect(jsonPath("$.preguntas[1].acertada").value(false));
        mvc.perform(comoEstudiante(post("/api/simulations/s-2/finish"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }

    @Test
    @DisplayName("GET /catalog/summary lo ven estudiantes, docentes y administradores")
    void catalogo() throws Exception {
        when(servicio.resumenDelCatalogo()).thenReturn(new ResumenDelCatalogo(3, Map.of("INGLES", 3), Map.of("BASICO", 3)));

        mvc.perform(comoEstudiante(get("/api/simulations/catalog/summary"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.porCompetencia.INGLES").value(3));
        mvc.perform(como(get("/api/simulations/catalog/summary"), "docente1", "TEACHER")).andExpect(status().isOk());
        mvc.perform(como(get("/api/simulations/catalog/summary"), "autor1", "AUTHOR")).andExpect(status().isForbidden());
    }
}
