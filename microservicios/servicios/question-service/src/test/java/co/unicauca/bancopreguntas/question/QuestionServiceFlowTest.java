package co.unicauca.bancopreguntas.question;

import co.unicauca.bancopreguntas.question.infrastructure.messaging.ReviewEventsListener;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

/**
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real
 * embebido, con el broker simulado. Comprueba el cableado (transacciones, publicación tras el commit,
 * eventos de entrada, roles, Flyway y Swagger) sin necesidad de Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "bancopreguntas.events.outbox.enabled=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("question-service de extremo a extremo")
class QuestionServiceFlowTest {

    private static final String ADMIN = "admin-1";
    private static final String REVISOR = "revisor-1";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ReviewEventsListener escucha;

    @MockitoBean
    private RabbitTemplate rabbit;

    private static String autorNuevo() {
        return "autor-" + UUID.randomUUID();
    }

    /** Los identificadores de la prueba indican el rol: admin-*, revisor-* o autor-* (por defecto). */
    private static String rolesDe(String usuario) {
        if (usuario.startsWith("admin")) {
            return "ADMIN";
        }
        if (usuario.startsWith("revisor")) {
            return "REVIEWER";
        }
        return "AUTHOR";
    }

    private static Map<String, Object> cuerpo(String nombre) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("nombre", nombre);
        c.put("contexto", "Una aplicación debe actualizar varias ventanas cuando cambian los datos de una pregunta.");
        c.put("enunciado", "¿Qué patrón notifica a varios objetos cuando cambia el estado de otro?");
        c.put("opcionA", "Factory");
        c.put("opcionB", "Observer");
        c.put("opcionC", "Singleton");
        c.put("opcionD", "Adapter");
        c.put("respuestaCorrecta", "B");
        c.put("justificacion", "Observer define una dependencia uno a muchos para notificar los cambios de estado.");
        c.put("bibliografia", "Gamma, E. et al. (1994). Design Patterns. Addison-Wesley.");
        c.put("competencia", "LECTURA_CRITICA");
        c.put("tema", "Patrones de diseño");
        c.put("subtema", "Observer");
        c.put("dificultad", "INTERMEDIO");
        return c;
    }

    private ResponseEntity<JsonNode> llamar(HttpMethod metodo, String ruta, String usuario, Object cuerpo) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        if (usuario != null) {
            cabeceras.set("X-User-Id", usuario);
            cabeceras.set("X-User-Roles", rolesDe(usuario));
        }
        return http.exchange(ruta, metodo, new HttpEntity<>(cuerpo, cabeceras), JsonNode.class);
    }

    private String crear(String usuario, String nombre) {
        ResponseEntity<JsonNode> respuesta = llamar(HttpMethod.POST, "/api/questions", usuario, cuerpo(nombre));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return respuesta.getBody().get("id").asText();
    }

    private JsonNode obtener(String id) {
        ResponseEntity<JsonNode> respuesta = llamar(HttpMethod.GET, "/api/questions/" + id, ADMIN, null);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody();
    }

    private static Message evento(String eventId, String tipo, String datos) {
        String sobre = """
                {"eventId":"%s","type":"%s","occurredAt":"2026-10-08T15:00:00Z","data":%s}
                """.formatted(eventId, tipo, datos);
        return MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
    }

    /** Lleva una pregunta recién creada hasta {@code EN_REVISION}, como lo hace el servicio de revisión. */
    private void enviarYAsignar(String id, String autor, String sufijo) {
        llamar(HttpMethod.POST, "/api/questions/" + id + "/submit", autor, null);
        escucha.alAsignarRevisores(evento("asignacion-" + id + sufijo, "review.reviewers-assigned",
                "{\"questionId\":\"%s\"}".formatted(id)));
    }

    private void completar(String id, String resultado, String sufijo) {
        escucha.alCompletarseLaRevision(evento("completa-" + id + sufijo, "review.completed",
                "{\"questionId\":\"%s\",\"result\":\"%s\"}".formatted(id, resultado)));
    }

    @Test
    @DisplayName("el ciclo completo: crear, enviar a revisión, asignar revisores, aprobar y publicar")
    void cicloCompleto() {
        String autor = autorNuevo();
        String id = crear(autor, "Patrón Observer");
        clearInvocations(rabbit);

        ResponseEntity<JsonNode> enviada = llamar(HttpMethod.POST, "/api/questions/" + id + "/submit", autor, null);
        assertThat(enviada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enviada.getBody().get("estado").asText()).isEqualTo("PENDIENTE_REVISION");

        ArgumentCaptor<String> rutas = ArgumentCaptor.forClass(String.class);
        verify(rabbit, atLeastOnce()).send(eq("bancopreguntas.events"), rutas.capture(), any(Message.class));
        assertThat(rutas.getAllValues()).containsExactlyInAnyOrder("question.state-changed", "question.submitted");

        ResponseEntity<JsonNode> pendientes = llamar(HttpMethod.GET, "/api/questions/pending-review?size=100", ADMIN, null);
        assertThat(pendientes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pendientes.getBody().get("contenido").findValuesAsText("id")).contains(id);

        escucha.alAsignarRevisores(evento("e-" + id + "-1", "review.reviewers-assigned",
                "{\"questionId\":\"%s\",\"authorId\":\"%s\",\"reviewerIds\":[\"r1\",\"r2\"],\"assignedBy\":\"admin\"}"
                        .formatted(id, autor)));
        assertThat(obtener(id).get("estado").asText()).isEqualTo("EN_REVISION");

        completar(id, "APPROVED", "-2");
        assertThat(obtener(id).get("estado").asText()).isEqualTo("APROBADA");

        clearInvocations(rabbit);
        ResponseEntity<JsonNode> publicada = llamar(HttpMethod.POST, "/api/questions/" + id + "/publish", ADMIN, null);
        assertThat(publicada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicada.getBody().get("estado").asText()).isEqualTo("PUBLICADA");
        ArgumentCaptor<String> rutasPublicacion = ArgumentCaptor.forClass(String.class);
        verify(rabbit, atLeastOnce()).send(eq("bancopreguntas.events"), rutasPublicacion.capture(), any(Message.class));
        assertThat(rutasPublicacion.getAllValues()).containsExactlyInAnyOrder("question.state-changed", "question.published");

        JsonNode finalPregunta = obtener(id);
        assertThat(finalPregunta.get("historialEstados").findValuesAsText("hacia"))
                .containsExactly("BORRADOR", "PENDIENTE_REVISION", "EN_REVISION", "APROBADA", "PUBLICADA");
        assertThat(finalPregunta.get("historialEstados").get(0).get("desde").isNull()).isTrue();
        assertThat(finalPregunta.get("historialEstados").get(2).get("cambiadoPor").isNull()).isTrue();
        assertThat(finalPregunta.get("historialEstados").get(4).get("cambiadoPor").asText()).isEqualTo(ADMIN);
    }

    @Test
    @DisplayName("una pregunta rechazada se reabre como versión 2, se corrige y se vuelve a enviar")
    void rechazoReaperturaYReenvio() {
        String autor = autorNuevo();
        String id = crear(autor, "Pregunta a corregir");
        enviarYAsignar(id, autor, "-1");
        completar(id, "REJECTED", "-1");
        assertThat(obtener(id).get("estado").asText()).isEqualTo("RECHAZADA");

        clearInvocations(rabbit);
        ResponseEntity<JsonNode> reabierta = llamar(HttpMethod.POST, "/api/questions/" + id + "/reopen", autor, null);

        assertThat(reabierta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reabierta.getBody().get("estado").asText()).isEqualTo("BORRADOR");
        assertThat(reabierta.getBody().get("version").asInt()).isEqualTo(2);
        ArgumentCaptor<String> rutas = ArgumentCaptor.forClass(String.class);
        verify(rabbit, atLeastOnce()).send(eq("bancopreguntas.events"), rutas.capture(), any(Message.class));
        assertThat(rutas.getAllValues()).containsExactlyInAnyOrder("question.state-changed", "question.reopened");

        ResponseEntity<JsonNode> corregida = llamar(HttpMethod.PUT, "/api/questions/" + id, autor, cuerpo("Pregunta corregida"));
        assertThat(corregida.getStatusCode()).isEqualTo(HttpStatus.OK);

        clearInvocations(rabbit);
        ResponseEntity<JsonNode> reenviada = llamar(HttpMethod.POST, "/api/questions/" + id + "/submit", autor, null);
        assertThat(reenviada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reenviada.getBody().get("estado").asText()).isEqualTo("PENDIENTE_REVISION");
        assertThat(reenviada.getBody().get("version").asInt()).isEqualTo(2);

        List<String> hacia = obtener(id).get("historialEstados").findValuesAsText("hacia");
        assertThat(hacia).containsExactly("BORRADOR", "PENDIENTE_REVISION", "EN_REVISION", "RECHAZADA",
                "BORRADOR", "PENDIENTE_REVISION");
    }

    @Test
    @DisplayName("solo se reabre una pregunta rechazada y solo su autor")
    void reaperturaReglas() {
        String autor = autorNuevo();
        String id = crear(autor, "Pregunta en borrador");

        assertThat(llamar(HttpMethod.POST, "/api/questions/" + id + "/reopen", autor, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        enviarYAsignar(id, autor, "-1");
        completar(id, "REJECTED", "-1");
        assertThat(llamar(HttpMethod.POST, "/api/questions/" + id + "/reopen", autorNuevo(), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(obtener(id).get("estado").asText()).isEqualTo("RECHAZADA");
    }

    @Test
    @DisplayName("solo el administrador publica y solo una pregunta aprobada")
    void publicacionReglas() {
        String autor = autorNuevo();
        String id = crear(autor, "Pregunta sin aprobar");

        assertThat(llamar(HttpMethod.POST, "/api/questions/" + id + "/publish", autor, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.POST, "/api/questions/" + id + "/publish", REVISOR, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.POST, "/api/questions/" + id + "/publish", ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(obtener(id).get("estado").asText()).isEqualTo("BORRADOR");
    }

    @Test
    @DisplayName("el administrador encuentra las aprobadas de todos los autores para publicarlas")
    void busquedaDelAdministrador() {
        String autor = autorNuevo();
        String id = crear(autor, "Pregunta aprobada");
        enviarYAsignar(id, autor, "-1");
        completar(id, "APPROVED", "-1");
        crear(autorNuevo(), "Otra en borrador");

        ResponseEntity<JsonNode> aprobadas = llamar(HttpMethod.GET,
                "/api/questions/all?estado=APROBADA&size=100", ADMIN, null);

        assertThat(aprobadas.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aprobadas.getBody().get("contenido").findValuesAsText("id")).contains(id);
        assertThat(aprobadas.getBody().get("contenido").findValuesAsText("estado")).containsOnly("APROBADA");
        assertThat(llamar(HttpMethod.GET, "/api/questions/all", autor, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("una pregunta rechazada por un evento antes de enviarse a revisión no cambia")
    void eventoParaEstadoIncorrecto() {
        String autor = autorNuevo();
        String id = crear(autor, "Pregunta en borrador");

        completar(id, "REJECTED", "-x");

        assertThat(obtener(id).get("estado").asText()).isEqualTo("BORRADOR");
    }

    @Test
    @DisplayName("un evento repetido no vuelve a cambiar la pregunta")
    void eventoRepetido() {
        String autor = autorNuevo();
        String id = crear(autor, "Pregunta con evento repetido");
        llamar(HttpMethod.POST, "/api/questions/" + id + "/submit", autor, null);
        Message asignacion = evento("e-" + id, "review.reviewers-assigned", "{\"questionId\":\"%s\"}".formatted(id));

        escucha.alAsignarRevisores(asignacion);
        escucha.alAsignarRevisores(asignacion);

        JsonNode pregunta = obtener(id);
        assertThat(pregunta.get("estado").asText()).isEqualTo("EN_REVISION");
        assertThat(pregunta.get("historialEstados").size()).isEqualTo(3);
    }

    @Test
    @DisplayName("la validación estructural devuelve 400 con todas las violaciones y no guarda nada")
    void validacionEstructural() {
        String autor = autorNuevo();
        Map<String, Object> invalido = cuerpo("Pregunta inválida");
        invalido.put("opcionB", "Factory");
        invalido.put("enunciado", "¿Primera pregunta? ¿Segunda pregunta?");

        ResponseEntity<JsonNode> respuesta = llamar(HttpMethod.POST, "/api/questions", autor, invalido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().get("codigo").asText()).isEqualTo("VALIDACION_ESTRUCTURAL");
        assertThat(respuesta.getBody().get("detalles").size()).isGreaterThanOrEqualTo(2);
        ResponseEntity<JsonNode> mias = llamar(HttpMethod.GET, "/api/questions", autor, null);
        assertThat(mias.getBody().get("totalElementos").asInt()).isZero();
    }

    @Test
    @DisplayName("solo el autor modifica su borrador, y solo mientras sea Borrador")
    void modificarBorrador() {
        String autor = autorNuevo();
        String id = crear(autor, "Nombre original");

        ResponseEntity<JsonNode> modificada = llamar(HttpMethod.PUT, "/api/questions/" + id, autor, cuerpo("Nombre nuevo"));
        assertThat(modificada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(modificada.getBody().get("nombre").asText()).isEqualTo("Nombre nuevo");

        ResponseEntity<JsonNode> ajena = llamar(HttpMethod.PUT, "/api/questions/" + id, autorNuevo(), cuerpo("Robada"));
        assertThat(ajena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        llamar(HttpMethod.POST, "/api/questions/" + id + "/submit", autor, null);
        ResponseEntity<JsonNode> tarde = llamar(HttpMethod.PUT, "/api/questions/" + id, autor, cuerpo("Tarde"));
        assertThat(tarde.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(obtener(id).get("nombre").asText()).isEqualTo("Nombre nuevo");
    }

    @Test
    @DisplayName("el listado es solo del autor y filtra por texto sin distinguir tildes")
    void listadoDelAutor() {
        String autor = autorNuevo();
        String otro = autorNuevo();
        String id = crear(autor, "Educación vial");
        crear(autor, "Otra pregunta distinta");
        crear(otro, "Educación ajena");

        ResponseEntity<JsonNode> todas = llamar(HttpMethod.GET, "/api/questions", autor, null);
        ResponseEntity<JsonNode> filtradas = llamar(HttpMethod.GET, "/api/questions?texto=EDUCACION", autor, null);
        ResponseEntity<JsonNode> pagina = llamar(HttpMethod.GET, "/api/questions?page=1&size=1", autor, null);

        assertThat(todas.getBody().get("totalElementos").asInt()).isEqualTo(2);
        assertThat(filtradas.getBody().get("contenido").findValuesAsText("id")).containsExactly(id);
        assertThat(pagina.getBody().get("pagina").asInt()).isEqualTo(1);
        assertThat(pagina.getBody().get("contenido").size()).isEqualTo(1);
        assertThat(pagina.getBody().get("totalPaginas").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("un autor no ve las preguntas de otro, pero un revisor y el administrador sí")
    void visibilidad() {
        String autor = autorNuevo();
        String id = crear(autor, "Pregunta privada");

        assertThat(llamar(HttpMethod.GET, "/api/questions/" + id, autorNuevo(), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/questions/" + id, REVISOR, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(llamar(HttpMethod.GET, "/api/questions/" + id, ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("sin identidad responde 401, con un rol que no corresponde 403 y una pregunta inexistente 404")
    void erroresBasicos() {
        assertThat(llamar(HttpMethod.GET, "/api/questions", null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(llamar(HttpMethod.POST, "/api/questions", ADMIN, cuerpo("Un administrador no crea")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/questions/no-existe", ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("una ruta inexistente responde 404 y un método no permitido 405, no 500")
    void rutasYMetodos() {
        assertThat(llamar(HttpMethod.GET, "/api/no-existe", ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(llamar(HttpMethod.PUT, "/api/questions/x/submit", ADMIN, null).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("Swagger publica el contrato OpenAPI, con las cabeceras de identidad, y la interfaz de documentación")
    void swagger() {
        ResponseEntity<JsonNode> contrato = http.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(contrato.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode rutas = contrato.getBody().get("paths");
        assertThat(rutas.has("/api/questions")).isTrue();
        assertThat(rutas.has("/api/questions/{id}")).isTrue();
        assertThat(rutas.has("/api/questions/{id}/submit")).isTrue();
        assertThat(rutas.has("/api/questions/{id}/reopen")).isTrue();
        assertThat(rutas.has("/api/questions/{id}/publish")).isTrue();
        assertThat(rutas.has("/api/questions/pending-review")).isTrue();
        assertThat(rutas.has("/api/questions/all")).isTrue();
        assertThat(contrato.getBody().get("info").get("title").asText()).isEqualTo("question-service");

        List<String> cabeceras = rutas.get("/api/questions").get("post").get("parameters").findValuesAsText("name");
        assertThat(cabeceras).contains("X-User-Id", "X-User-Roles");
        assertThat(rutas.get("/api/questions").get("post").get("parameters").toString())
                .doesNotContain("AuthenticatedUser");

        ResponseEntity<String> interfaz = http.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(interfaz.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
