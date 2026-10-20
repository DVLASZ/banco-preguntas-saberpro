package co.unicauca.bancopreguntas.simulation;

import co.unicauca.bancopreguntas.simulation.infrastructure.messaging.QuestionEventsListener;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real embebido y el
 * broker simulado. Publica preguntas por evento, genera un simulacro, lo contesta y lo finaliza por la API, y
 * comprueba la calificación y el evento {@code simulation.completed} guardado en la bandeja de salida.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false",
                "simulation.expiration.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("simulation-service de extremo a extremo")
class SimulationServiceFlowTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private QuestionEventsListener escucha;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private RabbitTemplate rabbit;

    private ResponseEntity<JsonNode> llamar(HttpMethod metodo, String ruta, String usuario, String roles, String cuerpo) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.set("X-User-Id", usuario);
        cabeceras.set("X-User-Roles", roles);
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(ruta, metodo, new HttpEntity<>(cuerpo, cabeceras), JsonNode.class);
    }

    private static Message evento(String tipo, String datos) {
        String sobre = """
                {"eventId":"%s","type":"%s","occurredAt":"2026-10-16T15:00:00Z","data":%s}
                """.formatted(UUID.randomUUID(), tipo, datos);
        return MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
    }

    private void publicar(String id, String competencia, String dificultad, String correcta) {
        escucha.alPublicarseUnaPregunta(evento("question.published", """
                {"questionId":"%s","publishedBy":"admin","version":1,"title":"Pregunta %s","competency":"%s","topic":"Tema",
                 "subtopic":"Subtema","difficulty":"%s","context":"Contexto","statement":"¿Enunciado %s?",
                 "optionA":"A","optionB":"B","optionC":"C","optionD":"D","correctAnswer":"%s"}
                """.formatted(id, id, competencia, dificultad, id, correcta)));
    }

    @Test
    @DisplayName("del catálogo al resultado: genera, contesta, finaliza y publica simulation.completed")
    void cicloCompleto() {
        String unico = "FLUJO_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String p1 = UUID.randomUUID().toString();
        String p2 = UUID.randomUUID().toString();
        String p3 = UUID.randomUUID().toString();
        publicar(p1, unico, "BASICO", "B");
        publicar(p2, unico, "BASICO", "A");
        publicar(p3, unico, "AVANZADO", "D");

        // el catálogo refleja lo publicado
        JsonNode resumen = llamar(HttpMethod.GET, "/api/simulations/catalog/summary", "docente1", "TEACHER", null).getBody();
        assertThat(resumen.get("porCompetencia").get(unico).asInt()).isEqualTo(3);

        // pedir más preguntas de las que hay
        ResponseEntity<JsonNode> demasiadas = llamar(HttpMethod.POST, "/api/simulations", "estudiante1", "STUDENT",
                "{\"competencias\":[\"%s\"],\"cantidad\":5,\"duracionMinutos\":30}".formatted(unico));
        assertThat(demasiadas.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(demasiadas.getBody().get("codigo").asText()).isEqualTo("PREGUNTAS_INSUFICIENTES");

        // generar con filtro de dificultad
        ResponseEntity<JsonNode> creado = llamar(HttpMethod.POST, "/api/simulations", "estudiante1", "STUDENT",
                "{\"competencias\":[\"%s\"],\"dificultades\":[\"basico\"],\"cantidad\":2,\"duracionMinutos\":30}".formatted(unico));
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode simulacro = creado.getBody();
        String id = simulacro.get("id").asText();
        assertThat(simulacro.get("estado").asText()).isEqualTo("EN_CURSO");
        assertThat(simulacro.get("preguntas")).hasSize(2);
        assertThat(simulacro.get("preguntas").get(0).has("respuestaCorrecta")).isFalse();

        // otro estudiante no lo ve; el docente sí
        assertThat(llamar(HttpMethod.GET, "/api/simulations/" + id, "estudiante2", "STUDENT", null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/simulations/" + id, "docente1", "TEACHER", null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // contestar: la pregunta p1 tiene correcta B, la p2 tiene correcta A
        for (JsonNode pregunta : simulacro.get("preguntas")) {
            String qid = pregunta.get("preguntaId").asText();
            String elegida = qid.equals(p1) ? "B" : "C"; // acierta p1 y falla p2
            ResponseEntity<JsonNode> r = llamar(HttpMethod.POST, "/api/simulations/" + id + "/answers", "estudiante1",
                    "STUDENT", "{\"preguntaId\":\"%s\",\"opcion\":\"%s\"}".formatted(qid, elegida));
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        assertThat(llamar(HttpMethod.GET, "/api/simulations/" + id, "estudiante1", "STUDENT", null).getBody()
                .get("respondidas").asInt()).isEqualTo(2);

        // finalizar
        ResponseEntity<JsonNode> final_ = llamar(HttpMethod.POST, "/api/simulations/" + id + "/finish", "estudiante1", "STUDENT", null);
        assertThat(final_.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode terminado = final_.getBody();
        assertThat(terminado.get("estado").asText()).isEqualTo("FINALIZADO");
        assertThat(terminado.get("resultado").get("correctas").asInt()).isEqualTo(1);
        assertThat(terminado.get("resultado").get("puntaje").asDouble()).isEqualTo(50.0);
        assertThat(terminado.get("resultado").get("porCompetencia").get(0).get("competencia").asText()).isEqualTo(unico);
        assertThat(terminado.get("preguntas").get(0).get("respuestaCorrecta").asText()).isNotBlank();

        // finalizar otra vez no se permite
        assertThat(llamar(HttpMethod.POST, "/api/simulations/" + id + "/finish", "estudiante1", "STUDENT", null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // aparece en mi lista con su puntaje
        JsonNode lista = llamar(HttpMethod.GET, "/api/simulations", "estudiante1", "STUDENT", null).getBody();
        assertThat(lista.get("contenido").toString()).contains(id).contains("50.0");

        // el evento quedó en la bandeja de salida con su calificación
        String payload = jdbc.queryForObject("SELECT payload FROM outbox_event WHERE routing_key = 'simulation.completed' "
                + "AND payload LIKE ? ORDER BY seq DESC LIMIT 1", String.class, "%" + id + "%");
        assertThat(payload).contains("\"studentId\":\"estudiante1\"").contains("\"score\":50.0").contains(unico);
    }

    @Test
    @DisplayName("una pregunta archivada sale del catálogo y no entra en simulacros nuevos")
    void archivada() {
        String unico = "ARCH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qid = UUID.randomUUID().toString();
        publicar(qid, unico, "BASICO", "A");

        escucha.alCambiarElEstadoDeUnaPregunta(evento("question.state-changed",
                "{\"questionId\":\"%s\",\"from\":\"PUBLICADA\",\"to\":\"ARCHIVADA\",\"changedBy\":\"admin\"}".formatted(qid)));

        ResponseEntity<JsonNode> r = llamar(HttpMethod.POST, "/api/simulations", "estudiante1", "STUDENT",
                "{\"competencias\":[\"%s\"],\"cantidad\":1,\"duracionMinutos\":10}".formatted(unico));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("sin identidad responde 401 y con un rol ajeno 403")
    void seguridad() {
        assertThat(http.getForEntity("/api/simulations", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(llamar(HttpMethod.GET, "/api/simulations", "autor1", "AUTHOR", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
