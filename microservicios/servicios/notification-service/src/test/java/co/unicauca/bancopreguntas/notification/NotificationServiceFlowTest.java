package co.unicauca.bancopreguntas.notification;

import co.unicauca.bancopreguntas.notification.infrastructure.messaging.EventsListener;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real
 * embebido y el broker simulado. Reproduce la secuencia de eventos de una pregunta y comprueba lo que cada
 * usuario encuentra en su bandeja y los eventos {@code notification.created} que quedan en la bandeja de salida.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("notification-service de extremo a extremo")
class NotificationServiceFlowTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private EventsListener escucha;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private RabbitTemplate rabbit;

    private static String rolesDe(String usuario) {
        if (usuario.startsWith("admin")) {
            return "ADMIN";
        }
        return usuario.startsWith("rev") ? "REVIEWER" : "AUTHOR";
    }

    private ResponseEntity<JsonNode> llamar(HttpMethod metodo, String ruta, String usuario) {
        HttpHeaders cabeceras = new HttpHeaders();
        if (usuario != null) {
            cabeceras.set("X-User-Id", usuario);
            cabeceras.set("X-User-Roles", rolesDe(usuario));
        }
        return http.exchange(ruta, metodo, new HttpEntity<>(null, cabeceras), JsonNode.class);
    }

    private static Message evento(String tipo, String datos) {
        String sobre = """
                {"eventId":"%s","type":"%s","occurredAt":"2026-10-08T15:00:00Z","data":%s}
                """.formatted(UUID.randomUUID(), tipo, datos);
        return MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("la vida de una pregunta genera los avisos de cada usuario y un notification.created por cada uno")
    void cicloDeVida() {
        String q = uuid();
        String autor = "autor-" + uuid();
        String r1 = "rev-" + uuid();
        String r2 = "rev-" + uuid();

        escucha.alEnviarseUnaPregunta(evento("question.submitted",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"title\":\"Patrón Observer\",\"competency\":\"INGLES\",\"version\":1}"
                        .formatted(q, autor)));
        escucha.alAsignarseRevisores(evento("review.reviewers-assigned",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"round\":1,\"reviewerIds\":[\"%s\",\"%s\"],\"assignedBy\":\"admin-1\",\"automatic\":false}"
                        .formatted(q, autor, r1, r2)));
        escucha.alCompletarseUnaRevision(evento("review.completed",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"round\":1,\"result\":\"REJECTED\",\"observations\":[{\"reviewerId\":\"%s\",\"decision\":\"RECHAZAR\",\"text\":\"Hay dos respuestas posibles\"}]}"
                        .formatted(q, autor, r1)));
        escucha.alEnviarseUnaEvaluacion(evento("review.evaluation-submitted",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"round\":1,\"reviewerId\":\"%s\",\"decision\":\"APROBAR\",\"observation\":\"Yo la veo bien\",\"late\":true}"
                        .formatted(q, autor, r2)));

        // el revisor encuentra su aviso de asignación y luego el resultado de la revisión
        JsonNode delRevisor = llamar(HttpMethod.GET, "/api/notifications", r1).getBody();
        assertThat(delRevisor.get("contenido").findValuesAsText("tipo"))
                .containsExactly("RESULTADO_DE_REVISION", "REVISORES_ASIGNADOS");
        assertThat(delRevisor.get("contenido").get(1).get("asunto").asText()).contains("Patrón Observer");

        // el administrador que asignó recibe el resultado de la revisión
        JsonNode delAdministrador = llamar(HttpMethod.GET, "/api/notifications", "admin-1").getBody();
        assertThat(delAdministrador.get("contenido").findValuesAsText("tipo")).contains("RESULTADO_DE_REVISION");

        // el autor ve que su pregunta entró en revisión, el rechazo con la observación y la observación tardía, la más reciente primero
        JsonNode delAutor = llamar(HttpMethod.GET, "/api/notifications", autor).getBody();
        assertThat(delAutor.get("contenido").findValuesAsText("tipo"))
                .containsExactly("OBSERVACION_TARDIA", "REVISION_RECHAZADA", "PREGUNTA_EN_REVISION");
        assertThat(delAutor.get("contenido").get(1).get("mensaje").asText()).contains("Hay dos respuestas posibles");
        assertThat(llamar(HttpMethod.GET, "/api/notifications/unread-count", autor).getBody().get("noLeidas").asInt())
                .isEqualTo(3);

        // el autor lee una y luego el resto
        String id = delAutor.get("contenido").get(0).get("id").asText();
        assertThat(llamar(HttpMethod.POST, "/api/notifications/" + id + "/read", autor).getBody().get("leida").asBoolean())
                .isTrue();
        assertThat(llamar(HttpMethod.GET, "/api/notifications?soloNoLeidas=true", autor).getBody().get("contenido")).hasSize(2);
        assertThat(llamar(HttpMethod.POST, "/api/notifications/read-all", autor).getBody().get("marcadas").asInt()).isEqualTo(2);
        assertThat(llamar(HttpMethod.GET, "/api/notifications/unread-count", autor).getBody().get("noLeidas").asInt()).isZero();

        // cada aviso dejó su evento en la bandeja de salida, para que el email-service envíe el correo
        for (String usuario : new String[] {r1, r2, autor}) {
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM outbox_event WHERE routing_key = 'notification.created' AND payload LIKE ?",
                    Integer.class, "%\"recipientId\":\"" + usuario + "\"%")).isPositive();
        }
    }

    @Test
    @DisplayName("una pregunta publicada avisa a su autor, conocido por el envío a revisión")
    void publicacion() {
        String q = uuid();
        String autor = "autor-" + uuid();
        escucha.alEnviarseUnaPregunta(evento("question.submitted",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"title\":\"Observer\"}".formatted(q, autor)));

        escucha.alPublicarseUnaPregunta(evento("question.published",
                "{\"questionId\":\"%s\",\"publishedBy\":\"admin-1\"}".formatted(q)));

        JsonNode delAutor = llamar(HttpMethod.GET, "/api/notifications", autor).getBody();
        assertThat(delAutor.get("contenido").get(0).get("tipo").asText()).isEqualTo("PREGUNTA_PUBLICADA");
    }

    @Test
    @DisplayName("un evento repetido no duplica el aviso")
    void idempotente() {
        String q = uuid();
        String autor = "autor-" + uuid();
        Message aprobada = evento("review.completed",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"round\":1,\"result\":\"APPROVED\"}".formatted(q, autor));

        escucha.alCompletarseUnaRevision(aprobada);
        escucha.alCompletarseUnaRevision(aprobada);

        assertThat(llamar(HttpMethod.GET, "/api/notifications", autor).getBody().get("contenido")).hasSize(1);
    }

    @Test
    @DisplayName("nadie lee la bandeja ni las notificaciones de otro")
    void aislamiento() {
        String autor = "autor-" + uuid();
        escucha.alCompletarseUnaRevision(evento("review.completed",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"round\":1,\"result\":\"APPROVED\"}".formatted(uuid(), autor)));
        String id = llamar(HttpMethod.GET, "/api/notifications", autor).getBody().get("contenido").get(0).get("id").asText();

        assertThat(llamar(HttpMethod.GET, "/api/notifications", "autor-" + uuid()).getBody().get("contenido")).isEmpty();
        assertThat(llamar(HttpMethod.POST, "/api/notifications/" + id + "/read", "autor-" + uuid()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("sin identidad responde 401")
    void seguridad() {
        assertThat(llamar(HttpMethod.GET, "/api/notifications", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Swagger documenta el servicio")
    void swagger() {
        ResponseEntity<JsonNode> docs = http.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody().get("paths").has("/api/notifications/unread-count")).isTrue();
        assertThat(docs.getBody().get("info").get("title").asText()).isEqualTo("notification-service");
    }
}
