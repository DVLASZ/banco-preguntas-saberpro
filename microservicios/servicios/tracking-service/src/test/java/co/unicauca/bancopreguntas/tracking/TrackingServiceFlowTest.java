package co.unicauca.bancopreguntas.tracking;

import co.unicauca.bancopreguntas.tracking.infrastructure.messaging.SimulationEventsListener;
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
 * broker simulado. Entrega resultados de simulacros por evento y comprueba el historial, las estadísticas, las
 * fortalezas y debilidades y el reporte de grupo por la API.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("tracking-service de extremo a extremo")
class TrackingServiceFlowTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private SimulationEventsListener escucha;

    @MockitoBean
    private RabbitTemplate rabbit;

    private ResponseEntity<JsonNode> llamar(String ruta, String usuario, String roles) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.set("X-User-Id", usuario);
        cabeceras.set("X-User-Roles", roles);
        return http.exchange(ruta, HttpMethod.GET, new HttpEntity<>(null, cabeceras), JsonNode.class);
    }

    private void simulacroTerminado(String estudiante, String fin, int correctas, double puntaje, String porCompetencia) {
        String sobre = """
                {"eventId":"%s","type":"simulation.completed","occurredAt":"%s","data":{"simulationId":"%s","studentId":"%s",
                 "startedAt":"%s","finishedAt":"%s","durationSeconds":600,"expired":false,"total":10,"correct":%d,
                 "incorrect":%d,"unanswered":0,"score":%s,"byCompetency":%s}}
                """.formatted(UUID.randomUUID(), fin, UUID.randomUUID(), estudiante, fin, fin, correctas, 10 - correctas,
                puntaje, porCompetencia);
        Message m = MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
        escucha.alCompletarseUnSimulacro(m);
    }

    @Test
    @DisplayName("del evento al historial, las estadísticas, las fortalezas y el reporte de grupo")
    void cicloCompleto() {
        String ana = "ana-" + UUID.randomUUID();
        String luis = "luis-" + UUID.randomUUID();
        simulacroTerminado(ana, "2026-10-14T10:00:00Z", 4, 40.0,
                "[{\"competency\":\"LECTURA_CRITICA\",\"correct\":3,\"total\":5},{\"competency\":\"INGLES\",\"correct\":1,\"total\":5}]");
        simulacroTerminado(ana, "2026-10-15T10:00:00Z", 9, 90.0,
                "[{\"competency\":\"LECTURA_CRITICA\",\"correct\":5,\"total\":5},{\"competency\":\"INGLES\",\"correct\":4,\"total\":5}]");
        simulacroTerminado(luis, "2026-10-15T11:00:00Z", 5, 50.0,
                "[{\"competency\":\"LECTURA_CRITICA\",\"correct\":3,\"total\":5},{\"competency\":\"INGLES\",\"correct\":2,\"total\":5}]");

        // historial: el más reciente primero
        JsonNode historial = llamar("/api/tracking/me/history", ana, "STUDENT").getBody();
        assertThat(historial.get("totalElementos").asInt()).isEqualTo(2);
        assertThat(historial.get("contenido").get(0).get("puntaje").asDouble()).isEqualTo(90.0);

        // estadísticas individuales
        JsonNode stats = llamar("/api/tracking/me/stats", ana, "STUDENT").getBody();
        assertThat(stats.get("simulacros").asInt()).isEqualTo(2);
        assertThat(stats.get("puntajePromedio").asDouble()).isEqualTo(65.0);
        assertThat(stats.get("tendencia").asText()).isEqualTo("MEJORANDO");

        // fortalezas y debilidades: LECTURA_CRITICA 80 % (fortaleza), INGLES 50 % (intermedia)
        JsonNode competencias = llamar("/api/tracking/me/competencies", ana, "STUDENT").getBody();
        assertThat(competencias.get("fortalezas").get(0).get("competencia").asText()).isEqualTo("LECTURA_CRITICA");
        assertThat(competencias.get("debilidades")).isEmpty();
        assertThat(competencias.get("intermedias").get(0).get("competencia").asText()).isEqualTo("INGLES");

        // el docente consulta a un estudiante y el reporte del grupo
        assertThat(llamar("/api/tracking/students/" + ana + "/stats", "docente1", "TEACHER").getBody().get("simulacros").asInt())
                .isEqualTo(2);
        JsonNode grupo = llamar("/api/tracking/reports/group", "docente1", "TEACHER").getBody();
        assertThat(grupo.get("estudiantesDetalle").toString()).contains(ana).contains(luis);
        ResponseEntity<String> csv = http.exchange("/api/tracking/reports/group.csv", HttpMethod.GET,
                new HttpEntity<>(null, cabecerasDe("admin", "ADMIN")), String.class);
        assertThat(csv.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(csv.getBody()).startsWith("estudiante,simulacros,promedio").contains(ana + ",2,65.00");

        // un estudiante no ve a otro ni el reporte
        assertThat(llamar("/api/tracking/students/" + ana + "/stats", luis, "STUDENT").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar("/api/tracking/reports/group", luis, "STUDENT").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("un evento repetido no duplica el historial")
    void idempotente() {
        String ana = "ana-" + UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        String sobre = """
                {"eventId":"%s","type":"simulation.completed","data":{"simulationId":"%s","studentId":"%s",
                 "finishedAt":"2026-10-15T10:00:00Z","durationSeconds":60,"total":4,"correct":2,"score":50.0,"byCompetency":[]}}
                """.formatted(eventId, UUID.randomUUID(), ana);
        Message m = MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();

        escucha.alCompletarseUnSimulacro(m);
        escucha.alCompletarseUnSimulacro(m);

        assertThat(llamar("/api/tracking/me/stats", ana, "STUDENT").getBody().get("simulacros").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("sin identidad responde 401 y un estudiante sin simulacros recibe ceros")
    void seguridadYVacio() {
        assertThat(http.getForEntity("/api/tracking/me/stats", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        JsonNode stats = llamar("/api/tracking/me/stats", "nuevo-" + UUID.randomUUID(), "STUDENT").getBody();
        assertThat(stats.get("simulacros").asInt()).isZero();
        assertThat(stats.get("tendencia").asText()).isEqualTo("SIN_DATOS");
    }

    private static HttpHeaders cabecerasDe(String usuario, String roles) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.set("X-User-Id", usuario);
        cabeceras.set("X-User-Roles", roles);
        return cabeceras;
    }
}
