package co.unicauca.bancopreguntas.question;

import co.unicauca.bancopreguntas.question.application.port.CicloDeVidaDePreguntas;
import co.unicauca.bancopreguntas.question.application.port.EstadoDeCiclo;
import co.unicauca.bancopreguntas.question.application.port.ValidadorDeContenido;
import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException;
import co.unicauca.bancopreguntas.question.domain.validation.Violacion;
import co.unicauca.bancopreguntas.question.infrastructure.messaging.LifecycleEventsListener;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real embebido, con el
 * broker y los servicios de validación y de ciclo de vida simulados. Comprueba el cableado (transacciones, copia del
 * estado por eventos, roles, Flyway y Swagger) sin necesidad de Docker.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("question-service de extremo a extremo")
class QuestionServiceFlowTest {

    private static final String ADMIN = "admin-1";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private LifecycleEventsListener escucha;

    @MockitoBean
    private RabbitTemplate rabbit;

    @MockitoBean
    private ValidadorDeContenido validador;

    @MockitoBean
    private CicloDeVidaDePreguntas ciclo;

    private static String autorNuevo() {
        return "autor-" + UUID.randomUUID();
    }

    /** Los identificadores de la prueba indican el rol: admin-* o autor-* (por defecto). */
    private static String rolesDe(String usuario) {
        return usuario.startsWith("admin") ? "ADMIN" : "AUTHOR";
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
        when(ciclo.registrar(anyString())).thenReturn(new EstadoDeCiclo(EstadoPregunta.BORRADOR, 1, 1));
        ResponseEntity<JsonNode> respuesta = llamar(HttpMethod.POST, "/api/questions", usuario, cuerpo(nombre));
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return respuesta.getBody().get("id").asText();
    }

    private JsonNode obtener(String id) {
        ResponseEntity<JsonNode> respuesta = llamar(HttpMethod.GET, "/api/questions/" + id, ADMIN, null);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody();
    }

    private void avisar(String id, String hacia, int version, int secuencia) {
        String sobre = """
                {"eventId":"%s","type":"question.state-changed","occurredAt":"2026-10-08T15:00:00Z",
                 "data":{"questionId":"%s","from":"X","to":"%s","changedBy":null,"sequence":%d,"version":%d}}
                """.formatted(UUID.randomUUID(), id, hacia, secuencia, version);
        Message mensaje = MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8)).build();
        escucha.alCambiarElEstado(mensaje);
    }

    @Test
    @DisplayName("el ciclo completo: crear, enviar a revisión, recibir la revisión por aviso, reabrir y publicar")
    void cicloCompleto() {
        String autor = autorNuevo();
        String id = crear(autor, "Observer");
        assertThat(obtener(id).get("estado").asText()).isEqualTo("BORRADOR");

        when(ciclo.enviarARevision(any())).thenReturn(new EstadoDeCiclo(EstadoPregunta.PENDIENTE_REVISION, 1, 2));
        ResponseEntity<JsonNode> enviada = llamar(HttpMethod.POST, "/api/questions/" + id + "/submit", autor, null);
        assertThat(enviada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enviada.getBody().get("estado").asText()).isEqualTo("PENDIENTE_REVISION");

        // ya no se edita
        assertThat(llamar(HttpMethod.PUT, "/api/questions/" + id, autor, cuerpo("Otro nombre")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // la revisión la decide el servicio de revisión; lifecycle-service avisa y la copia se actualiza
        avisar(id, "EN_REVISION", 1, 3);
        avisar(id, "RECHAZADA", 1, 4);
        assertThat(obtener(id).get("estado").asText()).isEqualTo("RECHAZADA");

        // un aviso atrasado no hace retroceder la copia
        avisar(id, "EN_REVISION", 1, 3);
        assertThat(obtener(id).get("estado").asText()).isEqualTo("RECHAZADA");

        when(ciclo.reabrir(id)).thenReturn(new EstadoDeCiclo(EstadoPregunta.BORRADOR, 2, 5));
        ResponseEntity<JsonNode> reabierta = llamar(HttpMethod.POST, "/api/questions/" + id + "/reopen", autor, null);
        assertThat(reabierta.getBody().get("estado").asText()).isEqualTo("BORRADOR");
        assertThat(reabierta.getBody().get("version").asInt()).isEqualTo(2);
        assertThat(llamar(HttpMethod.PUT, "/api/questions/" + id, autor, cuerpo("Corregida")).getStatusCode()).isEqualTo(HttpStatus.OK);

        avisar(id, "APROBADA", 2, 9);
        when(ciclo.publicar(any())).thenReturn(new EstadoDeCiclo(EstadoPregunta.PUBLICADA, 2, 10));
        ResponseEntity<JsonNode> publicada = llamar(HttpMethod.POST, "/api/questions/" + id + "/publish", ADMIN, null);
        assertThat(publicada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicada.getBody().get("estado").asText()).isEqualTo("PUBLICADA");
    }

    @Test
    @DisplayName("lista las preguntas del autor, filtra por estado y las pendientes de revisión las ve el administrador")
    void listados() {
        String autor = autorNuevo();
        String a = crear(autor, "Primera");
        String b = crear(autor, "Segunda");
        when(ciclo.enviarARevision(any())).thenReturn(new EstadoDeCiclo(EstadoPregunta.PENDIENTE_REVISION, 1, 2));
        llamar(HttpMethod.POST, "/api/questions/" + b + "/submit", autor, null);

        JsonNode todas = llamar(HttpMethod.GET, "/api/questions", autor, null).getBody();
        assertThat(todas.get("totalElementos").asInt()).isEqualTo(2);
        JsonNode soloBorradores = llamar(HttpMethod.GET, "/api/questions?estado=BORRADOR", autor, null).getBody();
        assertThat(soloBorradores.get("contenido")).hasSize(1);
        assertThat(soloBorradores.get("contenido").get(0).get("id").asText()).isEqualTo(a);
        assertThat(llamar(HttpMethod.GET, "/api/questions/pending-review", ADMIN, null).getBody().toString()).contains(b);
    }

    @Test
    @DisplayName("el historial lo trae el ciclo de vida; si está caído la pregunta se muestra sin él")
    void historial() {
        String autor = autorNuevo();
        String id = crear(autor, "Historia");
        when(ciclo.historial(id)).thenReturn(List.of(
                new co.unicauca.bancopreguntas.question.domain.CambioEstado(null, EstadoPregunta.BORRADOR, autor,
                        java.time.Instant.parse("2026-10-08T10:00:00Z"))));
        assertThat(obtener(id).get("historialEstados")).hasSize(1);

        when(ciclo.historial(id)).thenThrow(new DependenciaNoDisponibleException("caído", null));
        JsonNode sinHistorial = obtener(id);
        assertThat(sinHistorial.get("id").asText()).isEqualTo(id);
        assertThat(sinHistorial.has("historialEstados")).isFalse();
    }

    @Test
    @DisplayName("un contenido inválido responde 400 con todos los campos; una dependencia caída, 503; y no se guarda nada")
    void errores() {
        doThrow(new QuestionValidationException(List.of(new Violacion("contexto", "El contexto es obligatorio"),
                new Violacion("opcionB", "Opciones repetidas")))).when(validador).validarOLanzar(any());
        ResponseEntity<JsonNode> invalida = llamar(HttpMethod.POST, "/api/questions", autorNuevo(), cuerpo("Mala"));
        assertThat(invalida.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalida.getBody().get("codigo").asText()).isEqualTo("VALIDACION_ESTRUCTURAL");
        assertThat(invalida.getBody().get("detalles")).hasSize(2);

        doThrow(new DependenciaNoDisponibleException("validación caída", null)).when(validador).validarOLanzar(any());
        ResponseEntity<JsonNode> caida = llamar(HttpMethod.POST, "/api/questions", autorNuevo(), cuerpo("Otra"));
        assertThat(caida.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(caida.getBody().get("codigo").asText()).isEqualTo("SERVICIO_NO_DISPONIBLE");
    }

    @Test
    @DisplayName("lo que el ciclo de vida rechaza se traduce a 409 y el autor ajeno recibe 403")
    void rechazosDelCiclo() {
        String autor = autorNuevo();
        String id = crear(autor, "Reglas");
        when(ciclo.publicar(any())).thenThrow(new OperacionNoPermitidaException("Solo se puede publicar una pregunta Aprobada"));

        ResponseEntity<JsonNode> sinAprobar = llamar(HttpMethod.POST, "/api/questions/" + id + "/publish", ADMIN, null);
        assertThat(sinAprobar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sinAprobar.getBody().get("codigo").asText()).isEqualTo("OPERACION_NO_PERMITIDA");
        assertThat(llamar(HttpMethod.POST, "/api/questions/" + id + "/submit", autorNuevo(), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("sin identidad responde 401, con un rol ajeno 403 y Swagger está disponible")
    void seguridadYDocumentacion() {
        assertThat(http.getForEntity("/api/questions", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(llamar(HttpMethod.POST, "/api/questions", ADMIN, cuerpo("Admin")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.getForEntity("/v3/api-docs", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
