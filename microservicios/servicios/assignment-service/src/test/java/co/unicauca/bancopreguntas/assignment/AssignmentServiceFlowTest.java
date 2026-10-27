package co.unicauca.bancopreguntas.assignment;

import co.unicauca.bancopreguntas.assignment.infrastructure.messaging.QuestionEventsListener;
import co.unicauca.bancopreguntas.assignment.infrastructure.messaging.ReviewEventsListener;
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
 * broker simulado. Entrega los eventos de preguntas y de evaluación, ejerce la API del administrador y del revisor, y
 * comprueba el evento {@code review.reviewers-assigned} guardado en la bandeja de salida.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false",
                "assignment.max-reviewers=3"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("assignment-service de extremo a extremo")
class AssignmentServiceFlowTest {

    private static final String ADMIN = "admin-1";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private QuestionEventsListener preguntas;

    @Autowired
    private ReviewEventsListener revision;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private RabbitTemplate rabbit;

    private static String rolesDe(String usuario) {
        if (usuario.startsWith("admin")) {
            return "ADMIN";
        }
        if (usuario.startsWith("rev")) {
            return "REVIEWER";
        }
        return "AUTHOR";
    }

    private ResponseEntity<JsonNode> llamar(HttpMethod metodo, String ruta, String usuario, String cuerpo) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        if (usuario != null) {
            cabeceras.set("X-User-Id", usuario);
            cabeceras.set("X-User-Roles", rolesDe(usuario));
        }
        return http.exchange(ruta, metodo, new HttpEntity<>(cuerpo, cabeceras), JsonNode.class);
    }

    private static Message evento(String tipo, String datos) {
        String sobre = """
                {"eventId":"%s","type":"%s","occurredAt":"2026-10-08T15:00:00Z","data":%s}
                """.formatted(UUID.randomUUID(), tipo, datos);
        return MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
    }

    private void preguntaEnviada(String pregunta, String autor, int version) {
        preguntas.alEnviarseUnaPregunta(evento("question.submitted",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"title\":\"Pregunta %s\",\"competency\":\"INGLES\",\"version\":%d}"
                        .formatted(pregunta, autor, pregunta, version)));
    }

    private void preguntaReabierta(String pregunta, String autor, int version) {
        preguntas.alReabrirseUnaPregunta(evento("question.reopened",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"version\":%d}".formatted(pregunta, autor, version)));
    }

    private void evaluada(String pregunta, int ronda, String revisor, String decision) {
        revision.alEvaluarseUnaPregunta(evento("review.evaluation-submitted",
                "{\"questionId\":\"%s\",\"authorId\":\"a\",\"round\":%d,\"reviewerId\":\"%s\",\"decision\":\"%s\",\"late\":false}"
                        .formatted(pregunta, ronda, revisor, decision)));
    }

    private void completada(String pregunta, int ronda, String resultado) {
        revision.alCompletarseLaRevision(evento("review.completed",
                "{\"questionId\":\"%s\",\"authorId\":\"a\",\"round\":%d,\"result\":\"%s\",\"observations\":[]}"
                        .formatted(pregunta, ronda, resultado)));
    }

    private ResponseEntity<JsonNode> asignar(String pregunta, String... revisores) {
        String lista = String.join("\",\"", revisores);
        return llamar(HttpMethod.POST, "/api/reviews/" + pregunta + "/assignments", ADMIN,
                "{\"reviewerIds\":[\"" + lista + "\"]}");
    }

    private List<String> publicados(String pregunta) {
        return jdbc.queryForList("SELECT routing_key FROM outbox_event WHERE payload LIKE ? ORDER BY seq", String.class,
                "%" + pregunta + "%");
    }

    private static String pregunta() {
        return UUID.randomUUID().toString();
    }

    private static String autor() {
        return "autor-" + UUID.randomUUID();
    }

    private static String revisor() {
        return "rev-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("la pregunta llega, el administrador asigna, el revisor ve su bandeja y se actualiza con la evaluación")
    void cicloCompleto() {
        String q = pregunta();
        String a = autor();
        String r1 = revisor();
        String r2 = revisor();
        preguntaEnviada(q, a, 1);

        JsonNode pendientes = llamar(HttpMethod.GET, "/api/reviews/pending-assignment?size=100", ADMIN, null).getBody();
        assertThat(pendientes.get("contenido").toString()).contains(q);

        ResponseEntity<JsonNode> asignada = asignar(q, r1, r2);
        assertThat(asignada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asignada.getBody().get("rondas").get(0).get("estado").asText()).isEqualTo("EN_CURSO");
        assertThat(asignada.getBody().get("rondas").get(0).get("revisores")).hasSize(2);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/pending-assignment?size=100", ADMIN, null).getBody().toString())
                .doesNotContain(q);

        JsonNode bandeja = llamar(HttpMethod.GET, "/api/reviews/assigned", r1, null).getBody();
        assertThat(bandeja.get("contenido")).hasSize(1);
        assertThat(bandeja.get("contenido").get(0).get("preguntaId").asText()).isEqualTo(q);
        assertThat(bandeja.get("contenido").get(0).get("pendiente").asBoolean()).isTrue();

        // evaluation-service informa que r1 aprobó: pasa a evaluadas
        evaluada(q, 1, r1, "APROBAR");
        assertThat(llamar(HttpMethod.GET, "/api/reviews/assigned", r1, null).getBody().get("contenido")).isEmpty();
        JsonNode evaluadas = llamar(HttpMethod.GET, "/api/reviews/assigned?estado=EVALUADAS", r1, null).getBody();
        assertThat(evaluadas.get("contenido").get(0).get("miDecision").asText()).isEqualTo("APROBAR");

        // el evento de asignación quedó en la bandeja de salida con los datos que necesita quien evalúa
        String payload = jdbc.queryForObject("SELECT payload FROM outbox_event WHERE routing_key = 'review.reviewers-assigned' "
                + "AND payload LIKE ? ORDER BY seq DESC LIMIT 1", String.class, "%" + q + "%");
        assertThat(payload).contains("\"questionVersion\":1").contains("\"title\":\"Pregunta " + q + "\"")
                .contains(r1).contains(r2).contains("\"automatic\":false");
    }

    @Test
    @DisplayName("rechazo, reapertura y reenvío: la nueva ronda reasigna sola a los mismos revisores y lo publica")
    void rechazoYReenvio() {
        String q = pregunta();
        String a = autor();
        String r1 = revisor();
        String r2 = revisor();
        preguntaEnviada(q, a, 1);
        asignar(q, r1, r2);

        completada(q, 1, "REJECTED");
        assertThat(asignar(q, revisor()).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        // el revisor que no evaluó sigue viendo la pregunta como pendiente (observación tardía)
        assertThat(llamar(HttpMethod.GET, "/api/reviews/assigned", r2, null).getBody().get("contenido").get(0)
                .get("estadoRonda").asText()).isEqualTo("RECHAZADA");

        preguntaReabierta(q, a, 2);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/assigned", r2, null).getBody().get("contenido")).isEmpty();

        preguntaEnviada(q, a, 2);
        JsonNode bandeja = llamar(HttpMethod.GET, "/api/reviews/assigned", r2, null).getBody();
        assertThat(bandeja.get("contenido")).hasSize(1);
        assertThat(bandeja.get("contenido").get(0).get("ronda").asInt()).isEqualTo(2);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/pending-assignment?size=100", ADMIN, null).getBody().toString())
                .doesNotContain(q);
        assertThat(publicados(q)).containsExactly("review.reviewers-assigned", "review.reviewers-assigned");
    }

    @Test
    @DisplayName("el máximo configurado se respeta y se informa en /config; el autor no puede ser revisor")
    void reglasDeAsignacion() {
        String q = pregunta();
        String a = autor();
        preguntaEnviada(q, a, 1);

        JsonNode config = llamar(HttpMethod.GET, "/api/reviews/config", a, null).getBody();
        assertThat(config.get("minimoDeRevisores").asInt()).isEqualTo(1);
        assertThat(config.get("maximoDeRevisores").asInt()).isEqualTo(3);

        ResponseEntity<JsonNode> demasiados = asignar(q, revisor(), revisor(), revisor(), revisor());
        assertThat(demasiados.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(demasiados.getBody().get("codigo").asText()).isEqualTo("REVISION_INVALIDA");
        assertThat(asignar(q, a).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(asignar("no-existe", revisor()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("los roles se exigen en cada operación y sin identidad responde 401")
    void seguridad() {
        assertThat(llamar(HttpMethod.GET, "/api/reviews/pending-assignment", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/pending-assignment", revisor(), null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/assigned", ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.POST, "/api/reviews/" + pregunta() + "/assignments", revisor(),
                "{\"reviewerIds\":[\"x\"]}").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("un evento repetido no abre una segunda ronda")
    void eventoRepetido() {
        String q = pregunta();
        String a = autor();
        Message envio = evento("question.submitted",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"title\":\"t\",\"competency\":\"INGLES\",\"version\":1}".formatted(q, a));

        preguntas.alEnviarseUnaPregunta(envio);
        preguntas.alEnviarseUnaPregunta(envio);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM review_round r JOIN review_case c ON c.id = r.case_id "
                + "WHERE c.question_id = ?", Integer.class, q)).isEqualTo(1);
    }

    @Test
    @DisplayName("Swagger documenta el servicio")
    void swagger() {
        ResponseEntity<String> docs = http.getForEntity("/v3/api-docs", String.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody()).contains("/api/reviews/{questionId}/assignments").contains("assignment-service");
    }
}
