package co.unicauca.bancopreguntas.review;

import co.unicauca.bancopreguntas.review.infrastructure.messaging.QuestionEventsListener;
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
                "management.health.rabbit.enabled=false",
                "review.max-reviewers=4"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("review-service de extremo a extremo")
class ReviewServiceFlowTest {

    private static final String ADMIN = "admin-1";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private QuestionEventsListener escucha;

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

    private ResponseEntity<JsonNode> asignar(String pregunta, String... revisores) {
        String lista = String.join("\",\"", revisores);
        return llamar(HttpMethod.POST, "/api/reviews/" + pregunta + "/assignments", ADMIN,
                "{\"reviewerIds\":[\"" + lista + "\"]}");
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
    @DisplayName("el ciclo completo: la pregunta llega, el administrador asigna, los revisores aprueban")
    void cicloCompleto() {
        String q = pregunta();
        String autor = autor();
        String r1 = revisor();
        String r2 = revisor();
        preguntaEnviada(q, autor, 1);

        ResponseEntity<JsonNode> pendientes = llamar(HttpMethod.GET, "/api/reviews/pending-assignment?size=100", ADMIN, null);
        assertThat(pendientes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pendientes.getBody().get("contenido").findValuesAsText("preguntaId")).contains(q);

        clearInvocations(rabbit);
        ResponseEntity<JsonNode> asignada = asignar(q, r1, r2);
        assertThat(asignada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asignada.getBody().get("rondas").get(0).get("estado").asText()).isEqualTo("EN_CURSO");
        assertThat(rutasPublicadas()).containsExactly("review.reviewers-assigned");

        pendientes = llamar(HttpMethod.GET, "/api/reviews/pending-assignment?size=100", ADMIN, null);
        assertThat(pendientes.getBody().get("contenido").findValuesAsText("preguntaId")).doesNotContain(q);

        ResponseEntity<JsonNode> bandeja = llamar(HttpMethod.GET, "/api/reviews/assigned", r1, null);
        assertThat(bandeja.getBody().get("contenido").findValuesAsText("preguntaId")).containsExactly(q);

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

        assertThat(llamar(HttpMethod.GET, "/api/reviews/assigned?estado=EVALUADAS", r1, null).getBody()
                .get("contenido").findValuesAsText("preguntaId")).containsExactly(q);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/assigned", r1, null).getBody()
                .get("contenido")).isEmpty();
    }

    @Test
    @DisplayName("rechazo, observación tardía, reapertura y reenvío con los mismos revisores")
    void rechazoReaperturaYReenvio() {
        String q = pregunta();
        String autor = autor();
        String r1 = revisor();
        String r2 = revisor();
        String r3 = revisor();
        preguntaEnviada(q, autor, 1);
        asignar(q, r1, r2);

        // r1 rechaza: la ronda se decide sin esperar a r2
        clearInvocations(rabbit);
        ResponseEntity<JsonNode> rechazo = evaluar(q, r1, "RECHAZAR", "Hay dos respuestas posibles");
        assertThat(rechazo.getBody().get("resultadoDeLaRonda").asText()).isEqualTo("RECHAZADA");
        assertThat(rutasPublicadas()).containsExactly("review.evaluation-submitted", "review.completed");

        // el autor ya puede leer el rechazo; r2 todavía puede dejar su observación
        JsonNode paraElAutor = llamar(HttpMethod.GET, "/api/reviews/" + q, autor, null).getBody();
        assertThat(paraElAutor.get("rondas").get(0).get("detalleVisible").asBoolean()).isTrue();
        assertThat(llamar(HttpMethod.GET, "/api/reviews/assigned", r2, null).getBody().get("contenido")
                .findValuesAsText("preguntaId")).containsExactly(q);

        clearInvocations(rabbit);
        ResponseEntity<JsonNode> tardia = evaluar(q, r2, "APROBAR", "Yo la veo bien");
        assertThat(tardia.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tardia.getBody().get("tardia").asBoolean()).isTrue();
        assertThat(tardia.getBody().get("resultadoDeLaRonda").asText()).isEqualTo("PENDIENTE");
        assertThat(rutasPublicadas()).containsExactly("review.evaluation-submitted");

        // el autor reabre y reenvía: los mismos revisores quedan asignados solos
        preguntaReabierta(q, autor, 2);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/" + q, r1, null).getBody()
                .get("rondas").get(0).get("estado").asText()).isEqualTo("CERRADA");
        assertThat(evaluar(q, r2, "APROBAR", null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        clearInvocations(rabbit);
        preguntaEnviada(q, autor, 2);
        assertThat(rutasPublicadas()).containsExactly("review.reviewers-assigned");
        JsonNode vista = llamar(HttpMethod.GET, "/api/reviews/" + q, ADMIN, null).getBody();
        assertThat(vista.get("rondas")).hasSize(2);
        JsonNode segunda = vista.get("rondas").get(1);
        assertThat(segunda.get("estado").asText()).isEqualTo("EN_CURSO");
        assertThat(segunda.get("revisores").findValuesAsText("revisorId")).containsExactlyInAnyOrder(r1, r2);
        assertThat(segunda.get("revisores").findValuesAsText("asignadoPor")).containsOnly("system");
        // ya no espera al administrador
        assertThat(llamar(HttpMethod.GET, "/api/reviews/pending-assignment?size=100", ADMIN, null).getBody()
                .get("contenido").findValuesAsText("preguntaId")).doesNotContain(q);

        // el administrador solo puede agregar a un tercero, no quitar
        ResponseEntity<JsonNode> agregado = asignar(q, r3);
        assertThat(agregado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(agregado.getBody().get("rondas").get(1).get("revisores").findValuesAsText("revisorId"))
                .containsExactlyInAnyOrder(r1, r2, r3);

        // la ronda 2 se aprueba por unanimidad de los tres
        evaluar(q, r1, "APROBAR", null);
        evaluar(q, r2, "APROBAR", null);
        assertThat(evaluar(q, r3, "APROBAR", null).getBody().get("resultadoDeLaRonda").asText()).isEqualTo("APROBADA");
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
    @DisplayName("el máximo configurado se respeta y se informa en /config")
    void maximoDeRevisores() {
        String q = pregunta();
        preguntaEnviada(q, autor(), 1);

        ResponseEntity<JsonNode> config = llamar(HttpMethod.GET, "/api/reviews/config", autor(), null);
        assertThat(config.getBody().get("minimoDeRevisores").asInt()).isEqualTo(1);
        assertThat(config.getBody().get("maximoDeRevisores").asInt()).isEqualTo(4);

        ResponseEntity<JsonNode> demasiados = asignar(q, revisor(), revisor(), revisor(), revisor(), revisor());
        assertThat(demasiados.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(demasiados.getBody().get("codigo").asText()).isEqualTo("REVISION_INVALIDA");
        assertThat(asignar(q, revisor(), revisor(), revisor(), revisor()).getStatusCode()).isEqualTo(HttpStatus.OK);
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

        assertThat(llamar(HttpMethod.GET, "/api/reviews/pending-assignment", revisor(), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.POST, "/api/reviews/" + q + "/assignments", revisor(),
                "{\"reviewerIds\":[\"x\"]}").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/assigned", ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/reviews/config", null, null).getStatusCode())
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
        assertThat(docs.getBody().get("info").get("title").asText()).isEqualTo("review-service");
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
