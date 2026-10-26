package co.unicauca.bancopreguntas.validation.api;

import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import co.unicauca.bancopreguntas.validation.application.ValidationApplicationService;
import co.unicauca.bancopreguntas.validation.domain.validation.QuestionValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ValidationController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@Import(ValidationControllerTest.Config.class)
@DisplayName("ValidationController (API REST)")
class ValidationControllerTest {

    static class Config {
        @Bean
        ValidationApplicationService validationApplicationService() {
            return new ValidationApplicationService(QuestionValidator.porDefecto());
        }
    }

    @Autowired
    private MockMvc mvc;

    private static MockHttpServletRequestBuilder como(String usuario, String roles) {
        return post("/api/validation/questions").header("X-User-Id", usuario).header("X-User-Roles", roles)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private static final String VALIDA = """
            {"nombre":"Idea principal","contexto":"Un texto de contexto suficientemente claro para la pregunta.",
             "enunciado":"¿Cuál es la idea principal del texto?","opcionA":"Una idea sobre el tema del texto leído",
             "opcionB":"Otra idea distinta sobre el mismo tema","opcionC":"Una tercera idea posible del texto",
             "opcionD":"Una cuarta idea que no corresponde","respuestaCorrecta":"B","justificacion":"Porque sí lo dice el texto.",
             "bibliografia":"ICFES (2024)","competencia":"LECTURA_CRITICA","tema":"Idea principal","subtema":"Lectura",
             "dificultad":"BASICO"}
            """;

    @Test
    @DisplayName("un contenido válido responde 200 con valida=true")
    void valido() throws Exception {
        mvc.perform(como("autor1", "AUTHOR").content(VALIDA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.valida").value(true)).andExpect(jsonPath("$.violaciones.length()").value(0));
    }

    @Test
    @DisplayName("un contenido incompleto responde 200 con cada campo que incumple")
    void invalido() throws Exception {
        mvc.perform(como("autor1", "AUTHOR").content("{\"nombre\":\"\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.valida").value(false))
                .andExpect(jsonPath("$.violaciones[?(@.campo=='nombre')]").exists());
    }

    @Test
    @DisplayName("exige identidad y un rol AUTHOR o ADMIN")
    void seguridad() throws Exception {
        mvc.perform(post("/api/validation/questions").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isUnauthorized());
        mvc.perform(como("rev1", "REVIEWER").content(VALIDA)).andExpect(status().isForbidden());
        mvc.perform(como("admin", "ADMIN").content(VALIDA)).andExpect(status().isOk());
    }
}
