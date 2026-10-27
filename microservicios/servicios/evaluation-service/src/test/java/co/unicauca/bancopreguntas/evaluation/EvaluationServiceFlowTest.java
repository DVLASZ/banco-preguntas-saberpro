package co.unicauca.bancopreguntas.evaluation;

import co.unicauca.bancopreguntas.evaluation.infrastructure.messaging.AssignmentEventsListener;
import co.unicauca.bancopreguntas.evaluation.infrastructure.messaging.QuestionEventsListener;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

/**
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real
 * embebido, con el broker simulado. Comprueba el cableado (transacciones, publicación tras el commit,
 * eventos de entrada, roles, Flyway) y que dos evaluaciones simultáneas no decidan la ronda con datos viejos.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "bancopreguntas.events.outbox.enabled=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("evaluation-service de extremo a extremo")
class EvaluationServiceFlowTest {

    private static final String ADMIN = "admin-1";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private QuestionEventsListener escucha;

    @Autowired
    private AssignmentEventsListener asignaciones;

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

    private static Message evento(String eventId, String tipo, String datos) {
        String sobre = """
                {"eventId":"%s","type":"%s","occurredAt":"2026-10-08T15:00:00Z","data":%s}
                """.formatted(eventId, tipo, datos);
        return MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
    }

    private void preguntaEnviada(String pregunta, String autor, int version) {
        escucha.alEnviarseUnaPregunta(evento(UUID.randomUUID().toString(),
                "question.submitted",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"title\":\"Pregunta %s\",\"competency\":\"INGLES\",\"version\":%d}"
                        .formatted(pregunta, autor, pregunta, version)));
    }

    private void preguntaReabierta(String pregunta, String autor, int version) {
        escucha.alReabrirseUnaPregunta(evento(UUID.randomUUID().toString(),
                "question.reopened",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"version\":%d}".formatted(pregunta, autor, version)));
    }

    /** Simula el aviso de assignment-service: asigna revisores a la ronda vigente (la versión coincide con el número de ronda). */
    private void asignar(String pregunta, String... revisores) {
        int ronda = llamar(HttpMethod.GET, "/api/reviews/" + pregunta, ADMIN, null).getBody().get("rondas").size();
        asignarEnRonda(pregunta, ronda, false, revisores);
    }

    private void asignarEnRonda(String pregunta, int ronda, boolean automatica, String... revisores) {
        String lista = String.join("\",\"", revisores);
        asignaciones.alAsignarseRevisores(evento(UUID.randomUUID().toString(), "review.reviewers-assigned",
                "{\"questionId\":\"%s\",\"authorId\":\"a\",\"round\":%d,\"reviewerIds\":[\"%s\"],\"assignedBy\":\"%s\",\"automatic\":%s,\"questionVersion\":%d,\"title\":\"Pregunta\",\"competency\":\"INGLES\"}"
                        .formatted(pregunta, ronda, lista, automatica ? "system" : ADMIN, automatica, ronda)));
    }

    private ResponseEntity<JsonNode> evaluar(String pregunta, String revisor, String decision, String observacion) {
        String cuerpo = observacion == null
                ? "{\"decision\":\"%s\"}".formatted(decision)
                : "{\"decision\":\"%s\",\"observacion\":\"%s\"}".formatted(decision, observacion);
        return llamar(HttpMethod.POST, "/api/reviews/" + pregunta + "/evaluations", revisor, cuerpo);
    }

    private List<String> rutasPublicadas() {
        ArgumentCaptor<String> rutas = ArgumentCaptor.forClass(String.class);
        verify(rabbit, atLeast(0)).send(eq("bancopreguntas.events"), rutas.capture(), any(Message.class));
        return rutas.getAllValues();
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
    @DisplayName("el ciclo completo: la pregunta llega, se asignan los revisores y todos aprueban")
    void cicloCompleto() {
        String q = pregunta();
        String autor = autor();
        String r1 = revisor();
        String r2 = revisor();
        preguntaEnviada(q, autor, 1);

        JsonNode pendiente = llamar(HttpMethod.GET, "/api/reviews/" + q, ADMIN, null).getBody();
        assertThat(pendiente.get("rondas").get(0).get("estado").asText()).isEqualTo("PENDIENTE_ASIGNACION");
        assertThat(evaluar(q, r1, "APROBAR", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        asignar(q, r1, r2);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/" + q, ADMIN, null).getBody().get("rondas").get(0).get("estado").asText())
                .isEqualTo("EN_CURSO");

        clearInvocations(rabbit);
        ResponseEntity<JsonNode> primera = evaluar(q, r1, "APROBAR", null);
        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(primera.getBody().get("resultadoDeLaRonda").asText()).isEqualTo("PENDIENTE");
        ResponseEntity<JsonNode> segunda = evaluar(q, r2, "APROBAR", "Clara y bien formulada");
        assertThat(segunda.getBody().get("resultadoDeLaRonda").asText()).isEqualTo("APROBADA");
        assertThat(rutasPublicadas()).containsExactly("review.evaluation-submitted", "review.evaluation-submitted",
                "review.completed");

        JsonNode vista = llamar(HttpMethod.GET, "/api/reviews/" + q, ADMIN, null).getBody();
        assertThat(vista.get("rondas").get(0).get("estado").asText()).isEqualTo("APROBADA");
        assertThat(vista.get("rondas").get(0).get("revisores").findValuesAsText("decision")).containsExactly("APROBAR", "APROBAR");
    }

    @Test
    @DisplayName("el autor no ve el detalle mientras la ronda está en curso y un ajeno no ve nada")
    void visibilidad() {
        String q = pregunta();
        String autor = autor();
        String r1 = revisor();
        String r2 = revisor();
        preguntaEnviada(q, autor, 1);
        asignar(q, r1, r2);
        evaluar(q, r1, "APROBAR", "Bien");

        JsonNode paraElAutor = llamar(HttpMethod.GET, "/api/reviews/" + q, autor, null).getBody();
        assertThat(paraElAutor.get("rondas").get(0).get("detalleVisible").asBoolean()).isFalse();
        assertThat(paraElAutor.get("rondas").get(0).get("revisores")).isEmpty();
        assertThat(paraElAutor.get("rondas").get(0).get("totalDeEvaluadas").asInt()).isEqualTo(1);

        JsonNode paraElColega = llamar(HttpMethod.GET, "/api/reviews/" + q, r2, null).getBody();
        assertThat(paraElColega.get("rondas").get(0).get("revisores").get(0).get("evaluacion").get("observacion")
                .asText()).isEqualTo("Bien");

        assertThat(llamar(HttpMethod.GET, "/api/reviews/" + q, revisor(), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/" + q, autor(), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("rechazar sin observación, evaluar dos veces y evaluar sin ser asignado se rechazan")
    void reglasDeLaEvaluacion() {
        String q = pregunta();
        String r1 = revisor();
        String r2 = revisor();
        preguntaEnviada(q, autor(), 1);
        asignar(q, r1, r2);

        assertThat(evaluar(q, r1, "RECHAZAR", null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(evaluar(q, revisor(), "APROBAR", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(evaluar(q, r1, "APROBAR", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(evaluar(q, r1, "RECHAZAR", "Cambié de opinión").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(evaluar("q-no-existe", r1, "APROBAR", null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("los roles se exigen en cada operación y sin identidad responde 401")
    void roles() {
        String q = pregunta();
        preguntaEnviada(q, autor(), 1);

        assertThat(llamar(HttpMethod.POST, "/api/reviews/" + q + "/evaluations", ADMIN, "{\"decision\":\"APROBAR\"}").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/" + q, "estudiante-1", null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/" + q, null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("un evento repetido no abre una segunda ronda")
    void eventoRepetido() {
        String q = pregunta();
        Message mensaje = evento(UUID.randomUUID().toString(), "question.submitted",
                "{\"questionId\":\"%s\",\"authorId\":\"a\",\"title\":\"T\",\"competency\":\"INGLES\",\"version\":1}".formatted(q));

        escucha.alEnviarseUnaPregunta(mensaje);
        escucha.alEnviarseUnaPregunta(mensaje);

        JsonNode vista = llamar(HttpMethod.GET, "/api/reviews/" + q, ADMIN, null).getBody();
        assertThat(vista.get("rondas")).hasSize(1);
    }

    @Test
    @DisplayName("Swagger documenta el servicio")
    void swagger() {
        ResponseEntity<JsonNode> docs = http.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody().get("paths").has("/api/reviews/{questionId}/evaluations")).isTrue();
        assertThat(docs.getBody().get("info").get("title").asText()).isEqualTo("evaluation-service");
    }

    // ---------------------------------------------------------------- concurrencia

    private List<ResponseEntity<JsonNode>> evaluarAlMismoTiempo(String q, List<String> revisores, String decision) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(revisores.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<ResponseEntity<JsonNode>>> futuros = new ArrayList<>();
        for (String revisor : revisores) {
            futuros.add(pool.submit(() -> {
                salida.await();
                return evaluar(q, revisor, decision, decision.equals("RECHAZAR") ? "No cumple" : null);
            }));
        }
        salida.countDown();
        List<ResponseEntity<JsonNode>> respuestas = new ArrayList<>();
        for (Future<ResponseEntity<JsonNode>> f : futuros) {
            respuestas.add(f.get());
        }
        pool.shutdown();
        return respuestas;
    }

    @Test
    @DisplayName("tres revisores aprueban a la vez: la ronda se aprueba una sola vez")
    void aprobacionesSimultaneas() throws Exception {
        String q = pregunta();
        List<String> revisores = List.of(revisor(), revisor(), revisor());
        preguntaEnviada(q, autor(), 1);
        asignar(q, revisores.toArray(String[]::new));
        clearInvocations(rabbit);

        List<ResponseEntity<JsonNode>> respuestas = evaluarAlMismoTiempo(q, revisores, "APROBAR");

        assertThat(respuestas).allSatisfy(r -> assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK));
        assertThat(respuestas.stream().filter(r -> "APROBADA".equals(r.getBody().get("resultadoDeLaRonda").asText())))
                .as("exactamente una evaluación decide la ronda").hasSize(1);
        assertThat(rutasPublicadas().stream().filter("review.completed"::equals)).hasSize(1);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/" + q, ADMIN, null).getBody()
                .get("rondas").get(0).get("estado").asText()).isEqualTo("APROBADA");
    }

    @Test
    @DisplayName("tres revisores rechazan a la vez: el primero decide y los demás quedan como observaciones tardías")
    void rechazosSimultaneos() throws Exception {
        String q = pregunta();
        List<String> revisores = List.of(revisor(), revisor(), revisor());
        preguntaEnviada(q, autor(), 1);
        asignar(q, revisores.toArray(String[]::new));
        clearInvocations(rabbit);

        List<ResponseEntity<JsonNode>> respuestas = evaluarAlMismoTiempo(q, revisores, "RECHAZAR");

        assertThat(respuestas).allSatisfy(r -> assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK));
        assertThat(respuestas.stream().filter(r -> "RECHAZADA".equals(r.getBody().get("resultadoDeLaRonda").asText())))
                .hasSize(1);
        assertThat(respuestas.stream().filter(r -> r.getBody().get("tardia").asBoolean())).hasSize(2);
        assertThat(rutasPublicadas().stream().filter("review.completed"::equals)).hasSize(1);
    }

    @Test
    @DisplayName("el mismo revisor envía dos veces a la vez: solo una evaluación queda registrada")
    void dobleEnvioSimultaneo() throws Exception {
        String q = pregunta();
        String r1 = revisor();
        preguntaEnviada(q, autor(), 1);
        asignar(q, r1, revisor());

        List<ResponseEntity<JsonNode>> respuestas = evaluarAlMismoTiempo(q, List.of(r1, r1), "APROBAR");

        assertThat(respuestas.stream().filter(r -> r.getStatusCode() == HttpStatus.OK)).hasSize(1);
        assertThat(respuestas.stream().filter(r -> r.getStatusCode() == HttpStatus.CONFLICT)).hasSize(1);
    }
}
