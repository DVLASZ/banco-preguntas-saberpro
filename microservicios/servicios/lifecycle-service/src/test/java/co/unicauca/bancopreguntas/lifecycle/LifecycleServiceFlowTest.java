package co.unicauca.bancopreguntas.lifecycle;

import co.unicauca.bancopreguntas.lifecycle.infrastructure.messaging.ReviewEventsListener;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real embebido y el
 * broker simulado. Recorre el ciclo de una pregunta por la API y por los eventos de revisión, y comprueba el
 * historial y los eventos guardados en la bandeja de salida.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("lifecycle-service de extremo a extremo")
class LifecycleServiceFlowTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ReviewEventsListener revision;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private RabbitTemplate rabbit;

    private ResponseEntity<JsonNode> llamar(HttpMethod metodo, String ruta, String usuario, String roles, String cuerpo) {
        HttpHeaders cabeceras = new HttpHeaders();
        if (usuario != null) {
            cabeceras.set("X-User-Id", usuario);
            cabeceras.set("X-User-Roles", roles);
        }
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(ruta, metodo, new HttpEntity<>(cuerpo, cabeceras), JsonNode.class);
    }

    private static Message evento(String datos) {
        String sobre = """
                {"eventId":"%s","occurredAt":"2026-10-16T15:00:00Z","data":%s}
                """.formatted(UUID.randomUUID(), datos);
        return MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
    }

    private List<String> eventosGuardados(String id) {
        return jdbc.queryForList("SELECT routing_key FROM outbox_event WHERE payload LIKE ? ORDER BY seq", String.class,
                "%" + id + "%");
    }

    private static final String PUBLICACION = """
            {"titulo":"Observer","competencia":"LECTURA_CRITICA","tema":"Patrones","subtema":"Observer","dificultad":"BASICO",
             "contexto":"Contexto","enunciado":"¿Qué patrón?","opcionA":"a","opcionB":"b","opcionC":"c","opcionD":"d",
             "respuestaCorrecta":"B"}
            """;

    @Test
    @DisplayName("registra, envía, recibe la revisión, reabre, vuelve a pasar la revisión y publica")
    void cicloCompleto() {
        String id = UUID.randomUUID().toString();
        String base = "/api/lifecycle/questions";

        assertThat(llamar(HttpMethod.POST, base, "autor1", "AUTHOR", "{\"preguntaId\":\"%s\"}".formatted(id)).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(llamar(HttpMethod.POST, base, "autor2", "AUTHOR", "{\"preguntaId\":\"%s\"}".formatted(id)).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // otro autor no la mueve
        assertThat(llamar(HttpMethod.POST, base + "/" + id + "/submit", "autor2", "AUTHOR",
                "{\"titulo\":\"t\",\"competencia\":\"INGLES\"}").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<JsonNode> enviada = llamar(HttpMethod.POST, base + "/" + id + "/submit", "autor1", "AUTHOR",
                "{\"titulo\":\"Observer\",\"competencia\":\"LECTURA_CRITICA\"}");
        assertThat(enviada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enviada.getBody().get("estado").asText()).isEqualTo("PENDIENTE_REVISION");
        assertThat(enviada.getBody().get("editable").asBoolean()).isFalse();

        // no se publica sin aprobar
        assertThat(llamar(HttpMethod.POST, base + "/" + id + "/publish", "admin", "ADMIN", PUBLICACION).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // la revisión la deciden los eventos de revisión
        revision.alAsignarRevisores(evento("{\"questionId\":\"%s\"}".formatted(id)));
        revision.alCompletarseLaRevision(evento("{\"questionId\":\"%s\",\"result\":\"REJECTED\"}".formatted(id)));
        assertThat(llamar(HttpMethod.GET, base + "/" + id, "autor1", "AUTHOR", null).getBody().get("estado").asText())
                .isEqualTo("RECHAZADA");

        ResponseEntity<JsonNode> reabierta = llamar(HttpMethod.POST, base + "/" + id + "/reopen", "autor1", "AUTHOR", null);
        assertThat(reabierta.getBody().get("estado").asText()).isEqualTo("BORRADOR");
        assertThat(reabierta.getBody().get("version").asInt()).isEqualTo(2);

        llamar(HttpMethod.POST, base + "/" + id + "/submit", "autor1", "AUTHOR",
                "{\"titulo\":\"Observer\",\"competencia\":\"LECTURA_CRITICA\"}");
        revision.alAsignarRevisores(evento("{\"questionId\":\"%s\"}".formatted(id)));
        revision.alCompletarseLaRevision(evento("{\"questionId\":\"%s\",\"result\":\"APPROVED\"}".formatted(id)));

        ResponseEntity<JsonNode> publicada = llamar(HttpMethod.POST, base + "/" + id + "/publish", "admin", "ADMIN", PUBLICACION);
        assertThat(publicada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicada.getBody().get("estado").asText()).isEqualTo("PUBLICADA");
        assertThat(publicada.getBody().get("secuencia").asInt()).isEqualTo(9);

        // historial completo, en orden
        JsonNode historial = llamar(HttpMethod.GET, base + "/" + id + "/history", "admin", "ADMIN", null).getBody();
        assertThat(historial).hasSize(9);
        assertThat(historial.get(0).get("hacia").asText()).isEqualTo("BORRADOR");
        assertThat(historial.get(8).get("hacia").asText()).isEqualTo("PUBLICADA");
        assertThat(historial.get(2).get("cambiadoPor").isNull()).isTrue();

        // los eventos quedaron en la bandeja de salida, en orden
        assertThat(eventosGuardados(id)).containsExactly(
                "question.state-changed", "question.submitted",
                "question.state-changed", "question.state-changed",
                "question.state-changed", "question.reopened",
                "question.state-changed", "question.submitted",
                "question.state-changed", "question.state-changed",
                "question.state-changed", "question.published");
    }

    @Test
    @DisplayName("sin identidad responde 401 y con un rol ajeno 403")
    void seguridad() {
        assertThat(llamar(HttpMethod.GET, "/api/lifecycle/questions/x", null, null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(llamar(HttpMethod.GET, "/api/lifecycle/questions/x", "est1", "STUDENT", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
