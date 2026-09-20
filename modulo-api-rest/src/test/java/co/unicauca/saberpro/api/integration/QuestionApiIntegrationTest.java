package co.unicauca.saberpro.api.integration;

import co.unicauca.saberpro.api.repository.QuestionJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorre la API completa (controlador, servicio, dominio, JPA y H2) sin dobles de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
class QuestionApiIntegrationTest {

    private static final String CUERPO = """
            {"nombre":"%s","enunciado":"¿Qué patrón notifica a varios objetos cuando cambia un estado?",
             "opcionA":"Factory","opcionB":"Observer","opcionC":"Singleton","opcionD":"Adapter",
             "respuestaCorrecta":"B","competencia":"LECTURA_CRITICA","tema":"Patrones de diseño",
             "dificultad":"INTERMEDIO"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionJpaRepository jpa;

    private String crearPregunta(String nombre) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode cuerpo = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return cuerpo.get("id").asText();
    }

    @Test
    void postCreaLaPreguntaEnPendienteDeRevisionYSePersisteEnLaBaseDeDatos() throws Exception {
        String id = crearPregunta("Observer en la práctica");

        assertTrue(jpa.existsById(id));
        mockMvc.perform(get("/api/questions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Observer en la práctica"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.respuestaCorrecta").value("B"));
    }

    @Test
    void getListaIncluyeLaPreguntaCreada() throws Exception {
        String id = crearPregunta("Aparece en el listado");

        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].nombre").value("Aparece en el listado"));
    }

    @Test
    void cadaPreguntaNuevaRecibeUnIdDistinto() throws Exception {
        String primero = crearPregunta("Primera");
        String segundo = crearPregunta("Segunda");

        assertTrue(segundo.compareTo(primero) > 0, "el segundo id debe ser posterior al primero");
    }

    @Test
    void putActualizaElContenidoSinCambiarElEstado() throws Exception {
        String id = crearPregunta("Nombre original");

        mockMvc.perform(put("/api/questions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.formatted("Nombre editado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre editado"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));

        mockMvc.perform(get("/api/questions/" + id))
                .andExpect(jsonPath("$.nombre").value("Nombre editado"));
    }

    @Test
    void deleteArchivaLaPreguntaPeroNoLaBorraDeLaBaseDeDatos() throws Exception {
        String id = crearPregunta("Para archivar");
        long totalAntes = jpa.count();

        mockMvc.perform(delete("/api/questions/" + id)).andExpect(status().isNoContent());

        assertEquals(totalAntes, jpa.count(), "RNF-16: ninguna pregunta se elimina físicamente");
        mockMvc.perform(get("/api/questions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ARCHIVADA"));
        mockMvc.perform(delete("/api/questions/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void lasOperacionesSobreUnIdInexistenteRetornan404() throws Exception {
        mockMvc.perform(get("/api/questions/P-9999")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/questions/P-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.formatted("No existe")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/questions/P-9999")).andExpect(status().isNotFound());
    }

    @Test
    void postConDatosInvalidosRetorna400YNoGuardaNada() throws Exception {
        long totalAntes = jpa.count();

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO.formatted("").replace("\"respuestaCorrecta\":\"B\"", "\"respuestaCorrecta\":\"Z\"")))
                .andExpect(status().isBadRequest());

        assertEquals(totalAntes, jpa.count());
    }
}
