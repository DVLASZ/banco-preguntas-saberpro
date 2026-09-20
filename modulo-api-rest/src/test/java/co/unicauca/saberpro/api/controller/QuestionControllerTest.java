package co.unicauca.saberpro.api.controller;

import co.unicauca.saberpro.api.dto.QuestionRequest;
import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.api.service.IQuestionApiService;
import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionController.class)
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IQuestionApiService service;

    private static QuestionResponse respuesta(String id) {
        return new QuestionResponse(id, "Nombre " + id, "Enunciado " + id, "A1", "B1", "C1", "D1", "B",
                EstadoPregunta.PENDIENTE_REVISION, Competencia.INGLES, "Tema", Dificultad.BASICO);
    }

    private static QuestionRequest solicitudValida() {
        return new QuestionRequest("Nombre", "Enunciado", "A1", "B1", "C1", "D1", "B",
                Competencia.INGLES, "Tema", Dificultad.BASICO);
    }

    private String json(Object cuerpo) throws Exception {
        return objectMapper.writeValueAsString(cuerpo);
    }

    @Test
    void getListaTodasLasPreguntas() throws Exception {
        when(service.findAll()).thenReturn(List.of(respuesta("P-001"), respuesta("P-002")));

        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("P-001"))
                .andExpect(jsonPath("$[1].estado").value("PENDIENTE_REVISION"));
    }

    @Test
    void getPorIdDevuelveLaPregunta() throws Exception {
        when(service.findById("P-001")).thenReturn(respuesta("P-001"));

        mockMvc.perform(get("/api/questions/P-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("P-001"))
                .andExpect(jsonPath("$.respuestaCorrecta").value("B"))
                .andExpect(jsonPath("$.competencia").value("INGLES"));
    }

    @Test
    void getPorIdInexistenteRetorna404ConMensaje() throws Exception {
        when(service.findById("P-404")).thenThrow(new NoSuchElementException("No existe una pregunta con id P-404"));

        mockMvc.perform(get("/api/questions/P-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No existe una pregunta con id P-404"));
    }

    @Test
    void postCreaLaPreguntaYResponde201ConLocation() throws Exception {
        when(service.save(any(QuestionRequest.class))).thenReturn(respuesta("P-013"));

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(solicitudValida())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/questions/P-013")))
                .andExpect(jsonPath("$.id").value("P-013"));
    }

    @Test
    void postConDatosInvalidosRetorna400ConElDetalleDeCadaCampo() throws Exception {
        QuestionRequest invalida = new QuestionRequest("", "Enunciado", "A1", "B1", "C1", "D1", "Z",
                Competencia.INGLES, "Tema", Dificultad.BASICO);

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("nombre: El nombre es obligatorio")))
                .andExpect(jsonPath("$.details", hasItem("respuestaCorrecta: La respuesta correcta debe ser A, B, C o D")));

        verify(service, never()).save(any());
    }

    @Test
    void postConCompetenciaDesconocidaRetorna400ListandoLosValoresPermitidos() throws Exception {
        String cuerpo = """
                {"nombre":"N","enunciado":"E","opcionA":"a","opcionB":"b","opcionC":"c","opcionD":"d",
                 "respuestaCorrecta":"A","competencia":"MATEMATICAS","tema":"T","dificultad":"BASICO"}
                """;

        mockMvc.perform(post("/api/questions").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para 'competencia'"))
                .andExpect(jsonPath("$.details[0]", containsString("LECTURA_CRITICA")));
    }

    @Test
    void postConJsonMalFormadoRetorna400() throws Exception {
        mockMvc.perform(post("/api/questions").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no es un JSON válido"));
    }

    @Test
    void postCuandoElDominioRechazaLosDatosRetorna400() throws Exception {
        when(service.save(any(QuestionRequest.class)))
                .thenThrow(new IllegalArgumentException("Las 4 opciones son obligatorias"));

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(solicitudValida())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Las 4 opciones son obligatorias"));
    }

    @Test
    void putActualizaLaPregunta() throws Exception {
        when(service.update(eq("P-001"), any(QuestionRequest.class))).thenReturn(respuesta("P-001"));

        mockMvc.perform(put("/api/questions/P-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(solicitudValida())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("P-001"));
    }

    @Test
    void putSobrePreguntaInexistenteRetorna404() throws Exception {
        when(service.update(eq("P-404"), any(QuestionRequest.class)))
                .thenThrow(new NoSuchElementException("No existe una pregunta con id P-404"));

        mockMvc.perform(put("/api/questions/P-404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(solicitudValida())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteArchivaLaPreguntaYResponde204() throws Exception {
        mockMvc.perform(delete("/api/questions/P-001"))
                .andExpect(status().isNoContent());

        verify(service).delete("P-001");
    }

    @Test
    void deleteSobrePreguntaInexistenteRetorna404() throws Exception {
        doThrow(new NoSuchElementException("No existe una pregunta con id P-404")).when(service).delete("P-404");

        mockMvc.perform(delete("/api/questions/P-404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unErrorInesperadoRetorna500SinFiltrarDetallesInternos() throws Exception {
        when(service.findAll()).thenThrow(new IllegalStateException("detalle interno secreto"));

        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno del servidor"));
    }
}
