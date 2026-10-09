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

import static org.hamcrest.Matchers.hasItem;
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
            {"nombre":"%s",
             "contexto":"Una aplicación de escritorio debe avisar a varias ventanas cuando cambia una pregunta.",
             "enunciado":"¿Qué patrón notifica a varios objetos cuando cambia un estado?",
             "opcionA":"Factory","opcionB":"Observer","opcionC":"Singleton","opcionD":"Adapter",
             "respuestaCorrecta":"B",
             "justificacion":"Observer define una dependencia uno a muchos para notificar los cambios.",
             "bibliografia":"Gamma, E. et al. (1994). Design Patterns. Addison-Wesley.",
             "competencia":"LECTURA_CRITICA","tema":"Patrones de diseño","subtema":"Observer",
             "dificultad":"INTERMEDIO","autor":"%s"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionJpaRepository jpa;

    private static String cuerpo(String nombre, String autor) {
        return CUERPO.formatted(nombre, autor);
    }

    private String crearPregunta(String nombre) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(nombre, "autor1")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return json.get("id").asText();
    }

    @Test
    void postCreaElBorradorConTodosSusCamposYSePersisteEnLaBaseDeDatos() throws Exception {
        String id = crearPregunta("Observer en la práctica");

        assertTrue(jpa.existsById(id));
        mockMvc.perform(get("/api/questions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Observer en la práctica"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.respuestaCorrecta").value("B"))
                .andExpect(jsonPath("$.contexto").value("Una aplicación de escritorio debe avisar a varias ventanas cuando cambia una pregunta."))
                .andExpect(jsonPath("$.subtema").value("Observer"))
                .andExpect(jsonPath("$.autor").value("autor1"));
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
    void putActualizaElContenidoDelBorradorSinCambiarSuEstado() throws Exception {
        String id = crearPregunta("Nombre original");

        mockMvc.perform(put("/api/questions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Nombre editado", "autor1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre editado"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));

        mockMvc.perform(get("/api/questions/" + id))
                .andExpect(jsonPath("$.nombre").value("Nombre editado"));
    }

    @Test
    void putDeOtroAutorRetorna403YNoCambiaNada() throws Exception {
        String id = crearPregunta("Del autor uno");

        mockMvc.perform(put("/api/questions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Intento ajeno", "autor2")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/questions/" + id))
                .andExpect(jsonPath("$.nombre").value("Del autor uno"));
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
    void unaPreguntaArchivadaYaNoSePuedeModificarRetorna409() throws Exception {
        String id = crearPregunta("Se archiva y luego se intenta editar");
        mockMvc.perform(delete("/api/questions/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(put("/api/questions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Edición tardía", "autor1")))
                .andExpect(status().isConflict());
    }

    @Test
    void lasOperacionesSobreUnIdInexistenteRetornan404() throws Exception {
        mockMvc.perform(get("/api/questions/P-9999")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/questions/P-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("No existe", "autor1")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/questions/P-9999")).andExpect(status().isNotFound());
    }

    @Test
    void postConDatosInvalidosRetorna400YNoGuardaNada() throws Exception {
        long totalAntes = jpa.count();

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "autor1").replace("\"respuestaCorrecta\":\"B\"", "\"respuestaCorrecta\":\"Z\"")))
                .andExpect(status().isBadRequest());

        assertEquals(totalAntes, jpa.count());
    }

    @Test
    void postQueIncumpleLaValidacionEstructuralDelDominioRetorna400ConElDetalle() throws Exception {
        long totalAntes = jpa.count();
        String conFraseProhibida = cuerpo("Con frase prohibida", "autor1")
                .replace("\"opcionA\":\"Factory\"", "\"opcionA\":\"Todas las anteriores\"");

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conFraseProhibida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La pregunta no cumple la validación estructural"))
                .andExpect(jsonPath("$.details", hasItem("opcionA: La opción A usa una expresión no permitida (\"todas/ninguna de las anteriores\")")));

        assertEquals(totalAntes, jpa.count());
    }
}
