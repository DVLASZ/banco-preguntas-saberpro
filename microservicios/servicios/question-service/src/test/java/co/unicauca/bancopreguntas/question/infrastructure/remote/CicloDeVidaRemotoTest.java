package co.unicauca.bancopreguntas.question.infrastructure.remote;

import co.unicauca.bancopreguntas.question.application.port.EstadoDeCiclo;
import co.unicauca.bancopreguntas.question.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.Competencia;
import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.Dificultad;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.question.domain.Question;
import co.unicauca.bancopreguntas.question.domain.QuestionDistractors;
import co.unicauca.bancopreguntas.question.domain.QuestionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("CicloDeVidaRemoto (cliente de lifecycle-service)")
class CicloDeVidaRemotoTest {

    private static final String BASE = "http://ciclo.test/api/lifecycle/questions";

    private MockRestServiceServer servidor;
    private CicloDeVidaRemoto ciclo;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        ciclo = new CicloDeVidaRemoto(constructor, "http://ciclo.test");
    }

    private static Question pregunta() {
        return Question.builder().id("Q-1").nombre("Observer").contexto("Contexto").enunciado("¿Qué patrón?")
                .opciones(new QuestionDistractors("a", "b", "c", "d")).respuestaCorrecta('B').justificacion("j")
                .bibliografia("b").estado(EstadoPregunta.APROBADA).competencia(Competencia.LECTURA_CRITICA).tema("Patrones")
                .subtema("Observer").dificultad(Dificultad.BASICO).autor("autor-1").build();
    }

    private static final String ESTADO = "{\"preguntaId\":\"Q-1\",\"autorId\":\"autor-1\",\"estado\":\"%s\",\"version\":%d,\"secuencia\":%d,\"editable\":true}";

    @Test
    @DisplayName("registrar pide el registro y devuelve el estado inicial")
    void registrar() {
        servidor.expect(requestTo(BASE)).andExpect(method(HttpMethod.POST)).andExpect(jsonPath("$.preguntaId").value("Q-1"))
                .andRespond(withSuccess(ESTADO.formatted("BORRADOR", 1, 1), MediaType.APPLICATION_JSON));

        assertThat(ciclo.registrar("Q-1")).isEqualTo(new EstadoDeCiclo(EstadoPregunta.BORRADOR, 1, 1));
        servidor.verify();
    }

    @Test
    @DisplayName("enviar a revisión manda el título y la competencia")
    void enviar() {
        servidor.expect(requestTo(BASE + "/Q-1/submit")).andExpect(jsonPath("$.titulo").value("Observer"))
                .andExpect(jsonPath("$.competencia").value("LECTURA_CRITICA"))
                .andRespond(withSuccess(ESTADO.formatted("PENDIENTE_REVISION", 1, 2), MediaType.APPLICATION_JSON));

        assertThat(ciclo.enviarARevision(pregunta()).estado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
    }

    @Test
    @DisplayName("reabrir devuelve la versión nueva")
    void reabrir() {
        servidor.expect(requestTo(BASE + "/Q-1/reopen")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(ESTADO.formatted("BORRADOR", 2, 5), MediaType.APPLICATION_JSON));

        assertThat(ciclo.reabrir("Q-1")).isEqualTo(new EstadoDeCiclo(EstadoPregunta.BORRADOR, 2, 5));
    }

    @Test
    @DisplayName("publicar manda el contenido completo de la pregunta")
    void publicar() {
        servidor.expect(requestTo(BASE + "/Q-1/publish")).andExpect(jsonPath("$.enunciado").value("¿Qué patrón?"))
                .andExpect(jsonPath("$.opcionB").value("b")).andExpect(jsonPath("$.respuestaCorrecta").value("B"))
                .andExpect(jsonPath("$.dificultad").value("BASICO"))
                .andRespond(withSuccess(ESTADO.formatted("PUBLICADA", 1, 6), MediaType.APPLICATION_JSON));

        assertThat(ciclo.publicar(pregunta()).estado()).isEqualTo(EstadoPregunta.PUBLICADA);
    }

    @Test
    @DisplayName("el historial se convierte en cambios de estado, con el origen nulo de la creación")
    void historial() {
        servidor.expect(requestTo(BASE + "/Q-1/history")).andRespond(withSuccess("""
                [{"desde":null,"hacia":"BORRADOR","cambiadoPor":"autor-1","fecha":"2026-10-08T10:00:00Z"},
                 {"desde":"BORRADOR","hacia":"PENDIENTE_REVISION","cambiadoPor":"autor-1","fecha":"2026-10-08T10:05:00Z"}]
                """, MediaType.APPLICATION_JSON));

        List<CambioEstado> historial = ciclo.historial("Q-1");

        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).desde()).isNull();
        assertThat(historial.get(1).hacia()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        assertThat(historial.get(1).fecha()).isEqualTo(Instant.parse("2026-10-08T10:05:00Z"));
    }

    @Test
    @DisplayName("los rechazos del otro servicio vuelven a ser las excepciones del dominio (403, 404 y 409)")
    void rechazos() {
        servidor.expect(requestTo(BASE + "/Q-1/reopen")).andRespond(withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON).body("{\"codigo\":\"OPERACION_NO_PERMITIDA\",\"mensaje\":\"No está rechazada\"}"));
        assertThatThrownBy(() -> ciclo.reabrir("Q-1")).isInstanceOf(OperacionNoPermitidaException.class).hasMessage("No está rechazada");

        servidor.reset();
        servidor.expect(requestTo(BASE + "/Q-1/reopen")).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON).body("{\"codigo\":\"ACCESO_DENEGADO\",\"mensaje\":\"No es suya\"}"));
        assertThatThrownBy(() -> ciclo.reabrir("Q-1")).isInstanceOf(AccesoDenegadoException.class);

        servidor.reset();
        servidor.expect(requestTo(BASE + "/Q-1/history")).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON).body("{\"codigo\":\"PREGUNTA_NO_ENCONTRADA\",\"mensaje\":\"No existe\"}"));
        assertThatThrownBy(() -> ciclo.historial("Q-1")).isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    @DisplayName("un error del otro servicio o una caída se informan como dependencia no disponible")
    void caidas() {
        servidor.expect(requestTo(BASE + "/Q-1/reopen")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> ciclo.reabrir("Q-1")).isInstanceOf(DependenciaNoDisponibleException.class);

        servidor.reset();
        servidor.expect(requestTo(BASE + "/Q-1/reopen")).andRespond(withException(new SocketTimeoutException("lento")));
        assertThatThrownBy(() -> ciclo.reabrir("Q-1")).isInstanceOf(DependenciaNoDisponibleException.class);

        servidor.reset();
        servidor.expect(requestTo(BASE + "/Q-1/reopen")).andRespond(withException(new IOException("sin conexión")));
        assertThatThrownBy(() -> ciclo.reabrir("Q-1")).isInstanceOf(DependenciaNoDisponibleException.class);
    }
}
