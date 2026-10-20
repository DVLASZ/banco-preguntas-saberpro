package co.unicauca.bancopreguntas.history;

import co.unicauca.bancopreguntas.history.infrastructure.messaging.EventsListener;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real
 * embebido y el broker simulado. Reproduce la secuencia de eventos de una pregunta y comprueba qué ve cada
 * rol y que la auditoría reúne también los inicios de sesión.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("history-service de extremo a extremo")
class HistoryServiceFlowTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private EventsListener escucha;

    @MockitoBean
    private RabbitTemplate rabbit;

    private static String rolesDe(String usuario) {
        if (usuario.startsWith("admin")) {
            return "ADMIN";
        }
        return usuario.startsWith("rev") ? "REVIEWER" : "AUTHOR";
    }

    private ResponseEntity<JsonNode> consultar(String ruta, String usuario) {
        HttpHeaders cabeceras = new HttpHeaders();
        if (usuario != null) {
            cabeceras.set("X-User-Id", usuario);
            cabeceras.set("X-User-Roles", rolesDe(usuario));
        }
        return http.exchange(ruta, HttpMethod.GET, new HttpEntity<>(null, cabeceras), JsonNode.class);
    }

    private static Message evento(String tipo, int segundos, String datos) {
        String sobre = """
                {"eventId":"%s","type":"%s","occurredAt":"%s","data":%s}
                """.formatted(UUID.randomUUID(), tipo, Instant.parse("2026-10-08T10:00:00Z").plusSeconds(segundos), datos);
        return MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    /** Envío, asignación de dos revisores, un rechazo, el cierre de la ronda y una observación tardía. */
    private void recorrido(String q, String autor, String r1, String r2) {
        escucha.alEnviarseUnaPregunta(evento("question.submitted", 1,
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"title\":\"Observer\",\"version\":1}".formatted(q, autor)));
        escucha.alCambiarDeEstado(evento("question.state-changed", 2,
                "{\"questionId\":\"%s\",\"from\":\"BORRADOR\",\"to\":\"PENDIENTE_REVISION\",\"changedBy\":\"%s\"}".formatted(q, autor)));
        escucha.alAsignarseRevisores(evento("review.reviewers-assigned", 3,
                "{\"questionId\":\"%s\",\"round\":1,\"reviewerIds\":[\"%s\",\"%s\"],\"assignedBy\":\"admin-1\",\"automatic\":false}"
                        .formatted(q, r1, r2)));
        escucha.alEnviarseUnaEvaluacion(evento("review.evaluation-submitted", 4,
                "{\"questionId\":\"%s\",\"round\":1,\"reviewerId\":\"%s\",\"decision\":\"RECHAZAR\",\"late\":false}".formatted(q, r1)));
        escucha.alCompletarseUnaRevision(evento("review.completed", 5,
                "{\"questionId\":\"%s\",\"round\":1,\"result\":\"REJECTED\"}".formatted(q)));
        escucha.alEnviarseUnaEvaluacion(evento("review.evaluation-submitted", 6,
                "{\"questionId\":\"%s\",\"round\":1,\"reviewerId\":\"%s\",\"decision\":\"APROBAR\",\"late\":true}".formatted(q, r2)));
    }

    @Test
    @DisplayName("cada rol ve su parte del rastro de una pregunta")
    void visibilidadPorRol() {
        String q = uuid();
        String autor = "autor-" + uuid();
        String r1 = "rev-" + uuid();
        String r2 = "rev-" + uuid();
        recorrido(q, autor, r1, r2);

        JsonNode delAdmin = consultar("/api/history/questions/" + q, "admin-1").getBody();
        JsonNode delRevisor = consultar("/api/history/questions/" + q, r2).getBody();
        JsonNode delAutor = consultar("/api/history/questions/" + q, autor).getBody();

        assertThat(delAdmin.findValuesAsText("tipo")).containsExactly("PREGUNTA_ENVIADA", "CAMBIO_DE_ESTADO",
                "REVISORES_ASIGNADOS", "EVALUACION_ENVIADA", "REVISION_COMPLETADA", "EVALUACION_TARDIA");
        assertThat(delRevisor).hasSize(6);
        assertThat(delAutor.findValuesAsText("tipo")).containsExactly("PREGUNTA_ENVIADA", "CAMBIO_DE_ESTADO",
                "REVISION_COMPLETADA", "EVALUACION_TARDIA");
        assertThat(delAdmin.get(0).get("detalles").get("title").asText()).isEqualTo("Observer");
        assertThat(delAdmin.get(5).get("resumen").asText()).contains(r2).contains("tardía");
    }

    @Test
    @DisplayName("quien no tiene relación con la pregunta recibe 404; sin identidad, 401")
    void ajenos() {
        String q = uuid();
        recorrido(q, "autor-" + uuid(), "rev-" + uuid(), "rev-" + uuid());

        assertThat(consultar("/api/history/questions/" + q, "autor-" + uuid()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(consultar("/api/history/questions/" + q, "rev-" + uuid()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(consultar("/api/history/questions/" + uuid(), "admin-1").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(consultar("/api/history/questions/" + q, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("/me reúne lo visible de varias preguntas, lo más reciente primero")
    void lomio() {
        String autor = "autor-" + uuid();
        recorrido(uuid(), autor, "rev-" + uuid(), "rev-" + uuid());
        recorrido(uuid(), autor, "rev-" + uuid(), "rev-" + uuid());
        recorrido(uuid(), "autor-" + uuid(), "rev-" + uuid(), "rev-" + uuid());

        JsonNode mio = consultar("/api/history/me?size=100", autor).getBody();

        assertThat(mio.get("totalElementos").asInt()).isEqualTo(8); // 4 visibles × 2 preguntas
        assertThat(mio.get("contenido").findValuesAsText("tipo")).doesNotContain("EVALUACION_ENVIADA", "REVISORES_ASIGNADOS");
    }

    @Test
    @DisplayName("la auditoría del administrador incluye los inicios de sesión y se puede filtrar")
    void auditoria() {
        String usuario = "autor-" + uuid();
        escucha.alIniciarSesion(evento("auth.login-succeeded", 10, "{\"userId\":\"%s\",\"ip\":\"10.0.0.1\"}".formatted(usuario)));
        escucha.alFallarElInicioDeSesion(evento("auth.login-failed", 11, "{\"userId\":\"%s\"}".formatted(usuario)));

        JsonNode todos = consultar("/api/history/audit?actorId=" + usuario, "admin-1").getBody();
        JsonNode fallidos = consultar("/api/history/audit?actorId=" + usuario + "&tipo=INICIO_DE_SESION_FALLIDO", "admin-1").getBody();

        assertThat(todos.get("contenido").findValuesAsText("tipo"))
                .containsExactly("INICIO_DE_SESION_FALLIDO", "INICIO_DE_SESION"); // lo más reciente primero
        assertThat(fallidos.get("contenido")).hasSize(1);
        assertThat(todos.get("contenido").get(1).get("detalles").get("ip").asText()).isEqualTo("10.0.0.1");
        assertThat(consultar("/api/history/audit", usuario).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(consultar("/api/history/me?size=100", usuario).getBody().get("contenido")).isEmpty(); // el login no es suyo para verlo
    }

    @Test
    @DisplayName("un evento repetido no duplica la entrada")
    void idempotente() {
        String q = uuid();
        Message enviada = evento("question.submitted", 1,
                "{\"questionId\":\"%s\",\"authorId\":\"a-1\",\"title\":\"T\"}".formatted(q));

        escucha.alEnviarseUnaPregunta(enviada);
        escucha.alEnviarseUnaPregunta(enviada);

        assertThat(consultar("/api/history/questions/" + q, "admin-1").getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Swagger documenta el servicio")
    void swagger() {
        ResponseEntity<JsonNode> docs = http.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody().get("paths").has("/api/history/audit")).isTrue();
        assertThat(docs.getBody().get("info").get("title").asText()).isEqualTo("history-service");
    }
}
