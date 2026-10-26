package co.unicauca.bancopreguntas.audit;

import co.unicauca.bancopreguntas.audit.infrastructure.messaging.EventsListener;
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
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real embebido y el broker
 * simulado. Entrega eventos de preguntas, de revisión y de inicio de sesión y comprueba lo que ve el administrador.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("audit-service de extremo a extremo")
class AuditServiceFlowTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private EventsListener escucha;

    @MockitoBean
    private RabbitTemplate rabbit;

    private ResponseEntity<JsonNode> consultar(String ruta, String usuario, String roles) {
        HttpHeaders cabeceras = new HttpHeaders();
        if (usuario != null) {
            cabeceras.set("X-User-Id", usuario);
            cabeceras.set("X-User-Roles", roles);
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

    @Test
    @DisplayName("la auditoría reúne los hechos de la pregunta y los inicios de sesión, y se puede filtrar")
    void auditoria() {
        String q = uuid();
        String usuario = "autor-" + uuid();
        escucha.alEnviarseUnaPregunta(evento("question.submitted", 1,
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"title\":\"Observer\",\"version\":1}".formatted(q, usuario)));
        escucha.alIniciarSesion(evento("auth.login-succeeded", 10, "{\"userId\":\"%s\",\"ip\":\"10.0.0.1\"}".formatted(usuario)));
        escucha.alFallarElInicioDeSesion(evento("auth.login-failed", 11, "{\"userId\":\"%s\"}".formatted(usuario)));

        JsonNode todos = consultar("/api/audit?actorId=" + usuario, "admin-1", "ADMIN").getBody();
        JsonNode fallidos = consultar("/api/audit?actorId=" + usuario + "&tipo=INICIO_DE_SESION_FALLIDO", "admin-1", "ADMIN").getBody();
        JsonNode deLaPregunta = consultar("/api/audit?preguntaId=" + q, "admin-1", "ADMIN").getBody();

        assertThat(todos.get("contenido").findValuesAsText("tipo"))
                .containsExactly("INICIO_DE_SESION_FALLIDO", "INICIO_DE_SESION", "PREGUNTA_ENVIADA"); // lo más reciente primero
        assertThat(fallidos.get("contenido")).hasSize(1);
        assertThat(todos.get("contenido").get(1).get("detalles").get("ip").asText()).isEqualTo("10.0.0.1");
        assertThat(deLaPregunta.get("contenido")).hasSize(1);
    }

    @Test
    @DisplayName("un evento repetido no duplica la entrada")
    void idempotente() {
        String q = uuid();
        Message enviada = evento("question.submitted", 1, "{\"questionId\":\"%s\",\"authorId\":\"a-1\",\"title\":\"T\"}".formatted(q));

        escucha.alEnviarseUnaPregunta(enviada);
        escucha.alEnviarseUnaPregunta(enviada);

        assertThat(consultar("/api/audit?preguntaId=" + q, "admin-1", "ADMIN").getBody().get("contenido")).hasSize(1);
    }

    @Test
    @DisplayName("solo el administrador consulta y sin identidad responde 401")
    void seguridad() {
        assertThat(consultar("/api/audit", "autor-1", "AUTHOR").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(consultar("/api/audit", "rev-1", "REVIEWER").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(consultar("/api/audit", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Swagger documenta el servicio")
    void swagger() {
        ResponseEntity<JsonNode> docs = http.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody().get("paths").has("/api/audit")).isTrue();
        assertThat(docs.getBody().get("info").get("title").asText()).isEqualTo("audit-service");
    }
}
