package co.unicauca.bancopreguntas.question.api;

import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import co.unicauca.bancopreguntas.question.application.QuestionApplicationService;
import co.unicauca.bancopreguntas.question.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.Competencia;
import co.unicauca.bancopreguntas.question.domain.Dificultad;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;
import co.unicauca.bancopreguntas.question.domain.QuestionDistractors;
import co.unicauca.bancopreguntas.question.domain.QuestionNotFoundException;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException;
import co.unicauca.bancopreguntas.question.domain.validation.Violacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("QuestionController (API REST)")
class QuestionControllerTest {

    private static final String AUTOR = "autor-1";
    private static final String ADMIN = "admin-1";
    private static final Instant MOMENTO = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private QuestionApplicationService servicio;

    private static Question pregunta(String id, EstadoPregunta estado) {
        return Question.builder()
                .id(id).nombre("Patrón Observer").contexto("Contexto de la pregunta")
                .enunciado("¿Qué patrón notifica a varios objetos?")
                .opciones(new QuestionDistractors("Factory", "Observer", "Singleton", "Adapter"))
                .respuestaCorrecta('B').justificacion("Define una dependencia uno a muchos")
                .bibliografia("Gamma et al. (1994)").estado(estado)
                .competencia(Competencia.LECTURA_CRITICA).tema("Patrones").subtema("Observer")
                .dificultad(Dificultad.INTERMEDIO).autor(AUTOR)
                .creadaEn(MOMENTO).actualizadaEn(MOMENTO).version(1)
                .build();
    }

    /** Aplica a la petición la identidad de un usuario con los roles indicados. */
    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder peticion, String usuario, String roles) {
        return peticion.header("X-User-Id", usuario).header("X-User-Roles", roles);
    }

    private static MockHttpServletRequestBuilder comoAutor(MockHttpServletRequestBuilder peticion) {
        return como(peticion, AUTOR, "AUTHOR");
    }

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder peticion) {
        return como(peticion, ADMIN, "ADMIN");
    }

    private Map<String, Object> cuerpoValido() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("nombre", "Patrón Observer");
        cuerpo.put("contexto", "Contexto de la pregunta");
        cuerpo.put("enunciado", "¿Qué patrón notifica a varios objetos?");
        cuerpo.put("opcionA", "Factory");
        cuerpo.put("opcionB", "Observer");
        cuerpo.put("opcionC", "Singleton");
        cuerpo.put("opcionD", "Adapter");
        cuerpo.put("respuestaCorrecta", "B");
        cuerpo.put("justificacion", "Define una dependencia uno a muchos");
        cuerpo.put("bibliografia", "Gamma et al. (1994)");
        cuerpo.put("competencia", "LECTURA_CRITICA");
        cuerpo.put("tema", "Patrones");
        cuerpo.put("subtema", "Observer");
        cuerpo.put("dificultad", "INTERMEDIO");
        return cuerpo;
    }

    private String texto(Map<String, Object> cuerpo) throws Exception {
        return json.writeValueAsString(cuerpo);
    }

    // ---------------------------------------------------------------- crear

    @Test
    @DisplayName("POST /api/questions crea la pregunta, responde 201 y la ubica con Location")
    void crea() throws Exception {
        when(servicio.crearBorrador(any(), eq(AUTOR))).thenReturn(pregunta("Q-1", EstadoPregunta.BORRADOR));

        mvc.perform(comoAutor(post("/api/questions"))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpoValido())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/questions/Q-1")))
                .andExpect(jsonPath("$.id").value("Q-1"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.autorId").value(AUTOR))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.opcionB").value("Observer"))
                .andExpect(jsonPath("$.historialEstados").doesNotExist());
    }

    @Test
    @DisplayName("sin identidad responde 401")
    void sinUsuario() throws Exception {
        mvc.perform(post("/api/questions").contentType(MediaType.APPLICATION_JSON).content(texto(cuerpoValido())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("crear exige el rol AUTHOR: un revisor o un administrador reciben 403")
    void crearExigeAutor() throws Exception {
        mvc.perform(como(post("/api/questions"), "r-1", "REVIEWER")
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpoValido())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(comoAdmin(post("/api/questions"))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpoValido())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("un usuario sin roles no puede crear")
    void sinRoles() throws Exception {
        mvc.perform(post("/api/questions").header("X-User-Id", "u-1")
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpoValido())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un campo en blanco responde 400 con el campo en los detalles")
    void campoEnBlanco() throws Exception {
        Map<String, Object> cuerpo = cuerpoValido();
        cuerpo.put("nombre", " ");

        mvc.perform(comoAutor(post("/api/questions"))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_ENTRADA"))
                .andExpect(jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.startsWith("nombre:")));
    }

    @Test
    @DisplayName("una respuesta correcta fuera de A-D responde 400")
    void respuestaFueraDeRango() throws Exception {
        Map<String, Object> cuerpo = cuerpoValido();
        cuerpo.put("respuestaCorrecta", "E");

        mvc.perform(comoAutor(post("/api/questions"))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.containsString("A, B, C o D")));
    }

    @Test
    @DisplayName("la validación estructural del dominio responde 400 con cada violación")
    void validacionEstructural() throws Exception {
        when(servicio.crearBorrador(any(), eq(AUTOR))).thenThrow(new QuestionValidationException(List.of(
                new Violacion("contexto", "La pregunta debe tener un contexto"),
                new Violacion("opciones", "Debe haber exactamente cuatro distractores"))));

        mvc.perform(comoAutor(post("/api/questions"))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpoValido())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_ESTRUCTURAL"))
                .andExpect(jsonPath("$.detalles.length()").value(2))
                .andExpect(jsonPath("$.detalles[0]").value("contexto: La pregunta debe tener un contexto"));
    }

    @Test
    @DisplayName("un valor de competencia desconocido responde 400")
    void competenciaDesconocida() throws Exception {
        Map<String, Object> cuerpo = cuerpoValido();
        cuerpo.put("competencia", "MATEMATICAS");

        mvc.perform(comoAutor(post("/api/questions"))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }

    // ---------------------------------------------------------------- listar

    @Test
    @DisplayName("GET /api/questions lista las del usuario con filtros y paginación base 0")
    void lista() throws Exception {
        when(servicio.buscarDelAutor(eq(AUTOR), any(), eq(3), eq(5))).thenReturn(
                new Pagina<>(List.of(pregunta("Q-1", EstadoPregunta.BORRADOR)), 3, 5, 11));

        mvc.perform(comoAutor(get("/api/questions"))
                        .param("estado", "BORRADOR").param("competencia", "LECTURA_CRITICA")
                        .param("texto", "observer").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value("Q-1"))
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.tamano").value(5))
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(3));

        ArgumentCaptor<FiltroPreguntas> filtro = ArgumentCaptor.forClass(FiltroPreguntas.class);
        verify(servicio).buscarDelAutor(eq(AUTOR), filtro.capture(), eq(3), eq(5));
        assertThat(filtro.getValue()).isEqualTo(
                new FiltroPreguntas(EstadoPregunta.BORRADOR, Competencia.LECTURA_CRITICA, "observer"));
    }

    @Test
    @DisplayName("listar mis preguntas exige el rol AUTHOR")
    void listarExigeAutor() throws Exception {
        mvc.perform(comoAdmin(get("/api/questions"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un tamaño de página inválido responde 400")
    void tamanoInvalido() throws Exception {
        mvc.perform(comoAutor(get("/api/questions")).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
        mvc.perform(comoAutor(get("/api/questions")).param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(comoAutor(get("/api/questions")).param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("un estado desconocido en el filtro responde 400")
    void estadoDesconocido() throws Exception {
        mvc.perform(comoAutor(get("/api/questions")).param("estado", "INVENTADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }

    @Test
    @DisplayName("GET /api/questions/pending-review lista las pendientes de revisión (solo ADMIN)")
    void pendientes() throws Exception {
        when(servicio.buscarPendientesDeRevision(1, 10)).thenReturn(
                new Pagina<>(List.of(pregunta("Q-7", EstadoPregunta.PENDIENTE_REVISION)), 1, 10, 1));

        mvc.perform(comoAdmin(get("/api/questions/pending-review")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.pagina").value(0));
        mvc.perform(comoAutor(get("/api/questions/pending-review"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/questions/all permite al administrador buscar en todos los autores")
    void todas() throws Exception {
        when(servicio.buscarTodas(eq("autor-9"), any(), eq(1), eq(10))).thenReturn(
                new Pagina<>(List.of(pregunta("Q-8", EstadoPregunta.APROBADA)), 1, 10, 1));

        mvc.perform(comoAdmin(get("/api/questions/all")).param("autor", "autor-9").param("estado", "APROBADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value("Q-8"));

        ArgumentCaptor<FiltroPreguntas> filtro = ArgumentCaptor.forClass(FiltroPreguntas.class);
        verify(servicio).buscarTodas(eq("autor-9"), filtro.capture(), eq(1), eq(10));
        assertThat(filtro.getValue().estado()).isEqualTo(EstadoPregunta.APROBADA);
        mvc.perform(comoAutor(get("/api/questions/all"))).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- consultar

    @Test
    @DisplayName("GET /api/questions/{id} devuelve la pregunta con su historial de estados")
    void obtieneConHistorial() throws Exception {
        when(servicio.obtener("Q-1")).thenReturn(pregunta("Q-1", EstadoPregunta.PENDIENTE_REVISION));
        when(servicio.historial("Q-1")).thenReturn(List.of(
                new CambioEstado(null, EstadoPregunta.BORRADOR, AUTOR, MOMENTO),
                new CambioEstado(EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION, AUTOR, MOMENTO)));

        mvc.perform(comoAutor(get("/api/questions/Q-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historialEstados.length()").value(2))
                .andExpect(jsonPath("$.historialEstados[0].hacia").value("BORRADOR"))
                .andExpect(jsonPath("$.historialEstados[1].desde").value("BORRADOR"));
    }

    @Test
    @DisplayName("un revisor y un administrador pueden ver cualquier pregunta; un autor, solo las suyas")
    void visibilidad() throws Exception {
        when(servicio.obtener("Q-1")).thenReturn(pregunta("Q-1", EstadoPregunta.EN_REVISION));
        when(servicio.historial("Q-1")).thenReturn(List.of());

        mvc.perform(como(get("/api/questions/Q-1"), "r-1", "REVIEWER")).andExpect(status().isOk());
        mvc.perform(comoAdmin(get("/api/questions/Q-1"))).andExpect(status().isOk());
        mvc.perform(como(get("/api/questions/Q-1"), "otro-autor", "AUTHOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("una pregunta inexistente responde 404")
    void noExistente() throws Exception {
        when(servicio.obtener("Q-9")).thenThrow(new QuestionNotFoundException("Q-9"));

        mvc.perform(comoAutor(get("/api/questions/Q-9")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PREGUNTA_NO_ENCONTRADA"));
    }

    // ---------------------------------------------------------------- modificar

    @Test
    @DisplayName("PUT /api/questions/{id} modifica la pregunta")
    void modifica() throws Exception {
        when(servicio.actualizarContenido(eq("Q-1"), any(), eq(AUTOR)))
                .thenReturn(pregunta("Q-1", EstadoPregunta.BORRADOR));

        mvc.perform(comoAutor(put("/api/questions/Q-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpoValido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("Q-1"));
    }

    @Test
    @DisplayName("modificar la pregunta de otro autor responde 403")
    void modificarAjena() throws Exception {
        when(servicio.actualizarContenido(eq("Q-1"), any(), eq("otro")))
                .thenThrow(new AccesoDenegadoException("Solo el autor puede modificarla"));

        mvc.perform(como(put("/api/questions/Q-1"), "otro", "AUTHOR")
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpoValido())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("modificar fuera del estado Borrador responde 409")
    void modificarFueraDeBorrador() throws Exception {
        when(servicio.actualizarContenido(eq("Q-1"), any(), eq(AUTOR)))
                .thenThrow(new OperacionNoPermitidaException("Solo se puede modificar en Borrador"));

        mvc.perform(comoAutor(put("/api/questions/Q-1"))
                        .contentType(MediaType.APPLICATION_JSON).content(texto(cuerpoValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }

    // ---------------------------------------------------------------- ciclo de vida

    @Test
    @DisplayName("POST /api/questions/{id}/submit envía la pregunta a revisión")
    void enviaARevision() throws Exception {
        when(servicio.enviarARevision("Q-1", AUTOR)).thenReturn(pregunta("Q-1", EstadoPregunta.PENDIENTE_REVISION));

        mvc.perform(comoAutor(post("/api/questions/Q-1/submit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));
    }

    @Test
    @DisplayName("enviar a revisión una pregunta que no está en Borrador responde 409")
    void enviarYaEnviada() throws Exception {
        when(servicio.enviarARevision("Q-1", AUTOR))
                .thenThrow(new OperacionNoPermitidaException("Solo se puede enviar un Borrador"));

        mvc.perform(comoAutor(post("/api/questions/Q-1/submit"))).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/questions/{id}/reopen devuelve la pregunta rechazada a Borrador como versión nueva")
    void reabre() throws Exception {
        Question reabierta = Question.builder()
                .id("Q-1").nombre("Patrón Observer").contexto("Contexto").enunciado("¿Qué patrón notifica?")
                .opciones(new QuestionDistractors("Factory", "Observer", "Singleton", "Adapter"))
                .respuestaCorrecta('B').justificacion("J").bibliografia("B").estado(EstadoPregunta.BORRADOR)
                .competencia(Competencia.INGLES).tema("T").subtema("S").dificultad(Dificultad.BASICO)
                .autor(AUTOR).creadaEn(MOMENTO).actualizadaEn(MOMENTO).version(2).build();
        when(servicio.reabrirParaCorregir("Q-1", AUTOR)).thenReturn(reabierta);

        mvc.perform(comoAutor(post("/api/questions/Q-1/reopen")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("reabrir exige el rol AUTHOR y que la pregunta esté rechazada")
    void reabrirReglas() throws Exception {
        mvc.perform(comoAdmin(post("/api/questions/Q-1/reopen"))).andExpect(status().isForbidden());
        verify(servicio, never()).reabrirParaCorregir(any(), any());

        when(servicio.reabrirParaCorregir("Q-1", AUTOR))
                .thenThrow(new OperacionNoPermitidaException("Solo se puede reabrir una pregunta Rechazada"));
        mvc.perform(comoAutor(post("/api/questions/Q-1/reopen"))).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/questions/{id}/publish publica una pregunta aprobada (solo ADMIN)")
    void publica() throws Exception {
        when(servicio.publicar("Q-1", ADMIN)).thenReturn(pregunta("Q-1", EstadoPregunta.PUBLICADA));

        mvc.perform(comoAdmin(post("/api/questions/Q-1/publish")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADA"));
        mvc.perform(comoAutor(post("/api/questions/Q-1/publish"))).andExpect(status().isForbidden());
        mvc.perform(como(post("/api/questions/Q-1/publish"), "r-1", "REVIEWER")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("publicar una pregunta que no está aprobada responde 409")
    void publicarSinAprobar() throws Exception {
        when(servicio.publicar("Q-1", ADMIN))
                .thenThrow(new OperacionNoPermitidaException("Solo se puede publicar una pregunta Aprobada"));

        mvc.perform(comoAdmin(post("/api/questions/Q-1/publish")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));
    }

    // ---------------------------------------------------------------- errores

    @Test
    @DisplayName("un error inesperado responde 500 sin filtrar detalles internos")
    void errorInesperado() throws Exception {
        when(servicio.obtener("Q-1")).thenThrow(new IllegalStateException("detalle interno secreto"));

        mvc.perform(comoAutor(get("/api/questions/Q-1")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secreto"))));
    }

    @Test
    @DisplayName("una ruta que no existe responde 404, no 500")
    void rutaInexistente() throws Exception {
        mvc.perform(comoAutor(get("/api/questions/Q-1/no-existe"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("un método HTTP no permitido responde 405, no 500")
    void metodoNoPermitido() throws Exception {
        mvc.perform(comoAutor(put("/api/questions/Q-1/submit"))).andExpect(status().isMethodNotAllowed());
    }
}
