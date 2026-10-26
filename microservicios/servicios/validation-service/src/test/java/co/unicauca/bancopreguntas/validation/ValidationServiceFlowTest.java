package co.unicauca.bancopreguntas.validation;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba de extremo a extremo: la aplicación Spring completa, sin base de datos ni broker. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("validation-service de extremo a extremo")
class ValidationServiceFlowTest {

    @Autowired
    private TestRestTemplate http;

    private ResponseEntity<JsonNode> validar(String usuario, String roles, String cuerpo) {
        HttpHeaders cabeceras = new HttpHeaders();
        if (usuario != null) {
            cabeceras.set("X-User-Id", usuario);
            cabeceras.set("X-User-Roles", roles);
        }
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/api/validation/questions", HttpMethod.POST, new HttpEntity<>(cuerpo, cabeceras), JsonNode.class);
    }

    @Test
    @DisplayName("revisa un contenido y reporta todas las violaciones juntas")
    void reportaTodo() {
        ResponseEntity<JsonNode> r = validar("autor1", "AUTHOR",
                "{\"nombre\":\"Algo\",\"enunciado\":\"Sin signos\",\"opcionA\":\"a\",\"respuestaCorrecta\":\"B\"}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("valida").asBoolean()).isFalse();
        assertThat(r.getBody().get("violaciones").size()).isGreaterThan(2);
    }

    @Test
    @DisplayName("sin identidad responde 401 y con un rol ajeno 403")
    void seguridad() {
        assertThat(validar(null, null, "{}").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(validar("rev1", "REVIEWER", "{}").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
