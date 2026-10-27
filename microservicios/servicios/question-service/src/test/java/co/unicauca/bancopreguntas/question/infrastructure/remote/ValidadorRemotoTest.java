package co.unicauca.bancopreguntas.question.infrastructure.remote;

import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.validation.ContenidoDePrueba;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ValidadorRemoto (cliente de validation-service)")
class ValidadorRemotoTest {

    private static final String URL = "http://validacion.test/api/validation/questions";

    private MockRestServiceServer servidor;
    private ValidadorRemoto validador;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        validador = new ValidadorRemoto(constructor, "http://validacion.test");
    }

    @Test
    @DisplayName("un contenido válido pasa; manda el contenido tal como lo escribió el autor")
    void valido() {
        servidor.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.nombre").value("Patrón Observer"))
                .andExpect(jsonPath("$.competencia").value("LECTURA_CRITICA"))
                .andRespond(withSuccess("{\"valida\":true,\"violaciones\":[]}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> validador.validarOLanzar(ContenidoDePrueba.valido().build())).doesNotThrowAnyException();
        servidor.verify();
    }

    @Test
    @DisplayName("un contenido inválido lanza la excepción con todas las violaciones")
    void invalido() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("""
                {"valida":false,"violaciones":[{"campo":"contexto","mensaje":"El contexto es obligatorio"},
                                              {"campo":"opcionB","mensaje":"Opciones repetidas"}]}
                """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> validador.validarOLanzar(ContenidoDePrueba.valido().build()))
                .isInstanceOfSatisfying(QuestionValidationException.class, e -> {
                    assertThat(e.getViolaciones()).hasSize(2);
                    assertThat(e.getViolaciones().get(0).campo()).isEqualTo("contexto");
                    assertThat(e.getViolaciones().get(1).mensaje()).isEqualTo("Opciones repetidas");
                });
    }

    @Test
    @DisplayName("si validation-service falla, se informa como dependencia no disponible")
    void caido() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> validador.validarOLanzar(ContenidoDePrueba.valido().build()))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @Test
    @DisplayName("un rechazo 4xx del servicio no se confunde con una caída")
    void rechazo() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                .body("{\"codigo\":\"ACCESO_DENEGADO\",\"mensaje\":\"Rol no permitido\"}"));

        assertThatThrownBy(() -> validador.validarOLanzar(ContenidoDePrueba.valido().build()))
                .isInstanceOf(co.unicauca.bancopreguntas.question.domain.AccesoDenegadoException.class);
    }
}
