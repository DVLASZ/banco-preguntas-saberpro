package co.unicauca.bancopreguntas.email;

import co.unicauca.bancopreguntas.email.infrastructure.messaging.EventsListener;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real embebido y el
 * broker simulado. Entrega eventos {@code notification.created} y comprueba lo que el administrador ve en el registro.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false",
                "email.domain=correo.test"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("email-service de extremo a extremo")
class EmailServiceFlowTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private EventsListener escucha;

    @MockitoBean
    private RabbitTemplate rabbit;

    private ResponseEntity<JsonNode> llamar(String ruta, String usuario, String roles) {
        HttpHeaders cabeceras = new HttpHeaders();
        if (usuario != null) {
            cabeceras.set("X-User-Id", usuario);
            cabeceras.set("X-User-Roles", roles);
        }
        return http.exchange(ruta, HttpMethod.GET, new HttpEntity<>(null, cabeceras), JsonNode.class);
    }

    private static Message evento(String eventId, String destinatario, String asunto) {
        String sobre = """
                {"eventId":"%s","type":"notification.created","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"notificationId":"%s","recipientId":"%s","kind":"REVISORES_ASIGNADOS","questionId":"q-1",
                         "subject":"%s","message":"Se te asignó una pregunta"}}
                """.formatted(eventId, UUID.randomUUID(), destinatario, asunto);
        return MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("cada notificación creada sale como correo a la dirección derivada del usuario y el administrador lo ve")
    void correosRegistrados() {
        String usuario = "rev-" + UUID.randomUUID();

        escucha.alCrearseUnaNotificacion(evento(UUID.randomUUID().toString(), usuario, "Pregunta por revisar"));

        JsonNode correos = llamar("/api/emails?size=100", "admin-1", "ADMIN").getBody();
        assertThat(correos.get("contenido").findValuesAsText("para")).contains(usuario + "@correo.test");
        assertThat(correos.get("contenido").findValuesAsText("estado")).contains("ENVIADO");
    }

    @Test
    @DisplayName("un evento repetido no duplica el correo")
    void idempotente() {
        String usuario = "rev-" + UUID.randomUUID();
        Message mensaje = evento(UUID.randomUUID().toString(), usuario, "Pregunta por revisar");

        escucha.alCrearseUnaNotificacion(mensaje);
        escucha.alCrearseUnaNotificacion(mensaje);

        JsonNode correos = llamar("/api/emails?size=100", "admin-1", "ADMIN").getBody();
        assertThat(correos.get("contenido").findValuesAsText("para").stream().filter(p -> p.equals(usuario + "@correo.test")))
                .hasSize(1);
    }

    @Test
    @DisplayName("solo el administrador consulta los correos y sin identidad responde 401")
    void seguridad() {
        assertThat(llamar("/api/emails", "autor-1", "AUTHOR").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar("/api/emails", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Swagger documenta el servicio")
    void swagger() {
        ResponseEntity<JsonNode> docs = http.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody().get("info").get("title").asText()).isEqualTo("email-service");
        assertThat(docs.getBody().get("paths").has("/api/emails")).isTrue();
    }
}
