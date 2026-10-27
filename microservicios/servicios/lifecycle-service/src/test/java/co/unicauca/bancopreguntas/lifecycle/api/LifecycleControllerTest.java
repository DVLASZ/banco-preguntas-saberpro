package co.unicauca.bancopreguntas.lifecycle.api;

import co.unicauca.bancopreguntas.lifecycle.application.LifecycleApplicationService;
import co.unicauca.bancopreguntas.lifecycle.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.lifecycle.domain.CambioEstado;
import co.unicauca.bancopreguntas.lifecycle.domain.CicloDeVida;
import co.unicauca.bancopreguntas.lifecycle.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.lifecycle.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.lifecycle.domain.PreguntaNoRegistradaException;
import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
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

@WebMvcTest(LifecycleController.class)
@ImportAutoConfiguration(PlatformWebAutoConfiguration.class)
@DisplayName("LifecycleController (API REST)")
class LifecycleControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final String BASE = "/api/lifecycle/questions";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LifecycleApplicationService servicio;

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder p, String usuario, String roles) {
        return p.header("X-User-Id", usuario).header("X-User-Roles", roles).contentType(MediaType.APPLICATION_JSON);
    }

    private static CicloDeVida ciclo(EstadoPregunta estado) {
        return new CicloDeVida("Q-1", "autor1", estado, 1, 2, T0);
    }

    private static final String PUBLICACION = """
            {"titulo":"Observer","competencia":"LECTURA_CRITICA","tema":"Patrones","dificultad":"BASICO",
             "enunciado":"¿Qué patrón?","opcionA":"a","opcionB":"b","opcionC":"c","opcionD":"d","respuestaCorrecta":"B"}
            """;

    @Test
    @DisplayName("POST / registra la pregunta con 201 a nombre de quien la crea; solo AUTHOR")
    void registrar() throws Exception {
        when(servicio.registrar("Q-1", "autor1")).thenReturn(new CicloDeVida("Q-1", "autor1", EstadoPregunta.BORRADOR, 1, 1, T0));

        mvc.perform(como(post(BASE), "autor1", "AUTHOR").content("{\"preguntaId\":\"Q-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.editable").value(true))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(como(post(BASE), "admin", "ADMIN").content("{\"preguntaId\":\"Q-1\"}")).andExpect(status().isForbidden());
        mvc.perform(como(post(BASE), "autor1", "AUTHOR").content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /{id} lo ven ADMIN y REVIEWER siempre y el AUTHOR solo si es suyo")
    void obtener() throws Exception {
        when(servicio.obtener("Q-1")).thenReturn(ciclo(EstadoPregunta.PENDIENTE_REVISION));

        mvc.perform(como(get(BASE + "/Q-1"), "autor1", "AUTHOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION")).andExpect(jsonPath("$.editable").value(false))
                .andExpect(jsonPath("$.secuencia").value(2));
        mvc.perform(como(get(BASE + "/Q-1"), "admin", "ADMIN")).andExpect(status().isOk());
        mvc.perform(como(get(BASE + "/Q-1"), "rev1", "REVIEWER")).andExpect(status().isOk());
        mvc.perform(como(get(BASE + "/Q-1"), "autor2", "AUTHOR")).andExpect(status().isForbidden());
        mvc.perform(como(get(BASE + "/Q-1"), "est1", "STUDENT")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /{id} de una pregunta que no existe responde 404")
    void noExiste() throws Exception {
        when(servicio.obtener("Q-X")).thenThrow(new PreguntaNoRegistradaException("Q-X"));

        mvc.perform(como(get(BASE + "/Q-X"), "admin", "ADMIN")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PREGUNTA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("GET /{id}/history devuelve los cambios de estado en orden")
    void historial() throws Exception {
        when(servicio.obtener("Q-1")).thenReturn(ciclo(EstadoPregunta.PENDIENTE_REVISION));
        when(servicio.historial("Q-1")).thenReturn(List.of(
                new CambioEstado(null, EstadoPregunta.BORRADOR, "autor1", T0),
                new CambioEstado(EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION, "autor1", T0.plusSeconds(60))));

        mvc.perform(como(get(BASE + "/Q-1/history"), "autor1", "AUTHOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].hacia").value("BORRADOR"))
                .andExpect(jsonPath("$[1].desde").value("BORRADOR"));
    }

    @Test
    @DisplayName("POST /{id}/submit envía a revisión con el título y la competencia; solo AUTHOR")
    void enviar() throws Exception {
        when(servicio.enviarARevision("Q-1", "autor1", "Observer", "LECTURA_CRITICA"))
                .thenReturn(ciclo(EstadoPregunta.PENDIENTE_REVISION));

        mvc.perform(como(post(BASE + "/Q-1/submit"), "autor1", "AUTHOR")
                        .content("{\"titulo\":\"Observer\",\"competencia\":\"LECTURA_CRITICA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));
        mvc.perform(como(post(BASE + "/Q-1/submit"), "admin", "ADMIN")
                .content("{\"titulo\":\"Observer\",\"competencia\":\"LECTURA_CRITICA\"}")).andExpect(status().isForbidden());
        mvc.perform(como(post(BASE + "/Q-1/submit"), "autor1", "AUTHOR").content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /{id}/reopen devuelve la pregunta a Borrador; las reglas del dominio se traducen a 403 y 409")
    void reabrir() throws Exception {
        when(servicio.reabrir("Q-1", "autor1")).thenReturn(new CicloDeVida("Q-1", "autor1", EstadoPregunta.BORRADOR, 2, 5, T0));
        mvc.perform(como(post(BASE + "/Q-1/reopen"), "autor1", "AUTHOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        when(servicio.reabrir("Q-2", "autor1")).thenThrow(new OperacionNoPermitidaException("no está rechazada"));
        mvc.perform(como(post(BASE + "/Q-2/reopen"), "autor1", "AUTHOR")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OPERACION_NO_PERMITIDA"));

        when(servicio.reabrir("Q-3", "autor1")).thenThrow(new AccesoDenegadoException("no es suyo"));
        mvc.perform(como(post(BASE + "/Q-3/reopen"), "autor1", "AUTHOR")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("POST /{id}/publish publica con el contenido recibido; solo ADMIN")
    void publicar() throws Exception {
        when(servicio.publicar(eq("Q-1"), eq("admin"), any())).thenReturn(ciclo(EstadoPregunta.PUBLICADA));

        mvc.perform(como(post(BASE + "/Q-1/publish"), "admin", "ADMIN").content(PUBLICACION))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADA"));
        verify(servicio).publicar(eq("Q-1"), eq("admin"), any());
        mvc.perform(como(post(BASE + "/Q-1/publish"), "autor1", "AUTHOR").content(PUBLICACION)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sin identidad responde 401 y sin tocar el servicio")
    void sinIdentidad() throws Exception {
        mvc.perform(get(BASE + "/Q-1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }
}
