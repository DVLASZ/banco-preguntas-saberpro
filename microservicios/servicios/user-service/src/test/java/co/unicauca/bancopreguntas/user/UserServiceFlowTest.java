package co.unicauca.bancopreguntas.user;

import co.unicauca.bancopreguntas.user.application.port.IdentityProvider;
import co.unicauca.bancopreguntas.user.domain.EventoDeSesion;
import co.unicauca.bancopreguntas.user.domain.UsuarioExterno;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba de extremo a extremo del servicio: la aplicación Spring completa sobre un PostgreSQL real
 * embebido, con Keycloak y el broker simulados. Comprueba la sincronización de usuarios, la publicación
 * de los inicios de sesión y los roles de cada operación.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "user.sync.enabled=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "bancopreguntas.events.outbox.enabled=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@DisplayName("user-service de extremo a extremo")
class UserServiceFlowTest {

    @Autowired
    private TestRestTemplate http;

    @MockitoBean
    private IdentityProvider keycloak;

    @MockitoBean
    private RabbitTemplate rabbit;

    private ResponseEntity<JsonNode> llamar(HttpMethod metodo, String ruta, String usuario, String roles) {
        HttpHeaders cabeceras = new HttpHeaders();
        if (usuario != null) {
            cabeceras.set("X-User-Id", usuario);
            cabeceras.set("X-User-Roles", roles);
        }
        return http.exchange(ruta, metodo, new HttpEntity<>(null, cabeceras), JsonNode.class);
    }

    private static UsuarioExterno externo(String username, boolean activo, String... roles) {
        return new UsuarioExterno("kc-" + username, username, "Nombre " + username, username + "@correo.test",
                Set.of(roles), activo);
    }

    @Test
    @DisplayName("sincroniza los usuarios, los lista por rol y devuelve el perfil propio")
    void sincronizaYLista() {
        when(keycloak.usuarios()).thenReturn(List.of(
                externo("flujo-admin", true, "ADMIN"),
                externo("flujo-ana", true, "AUTHOR"),
                externo("flujo-luis", true, "REVIEWER"),
                externo("flujo-marta", true, "REVIEWER", "AUTHOR"),
                externo("flujo-pedro", false, "REVIEWER")));
        when(keycloak.eventosDeSesion(any())).thenReturn(List.of());

        ResponseEntity<JsonNode> sync = llamar(HttpMethod.POST, "/api/users/sync", "flujo-admin", "ADMIN");
        assertThat(sync.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sync.getBody().get("usuariosCreados").asInt()).isEqualTo(5);

        JsonNode revisores = llamar(HttpMethod.GET, "/api/users?rol=REVIEWER&texto=flujo-", "flujo-admin", "ADMIN").getBody();
        assertThat(revisores.get("contenido").findValuesAsText("id")).containsExactly("flujo-luis", "flujo-marta");
        JsonNode todos = llamar(HttpMethod.GET, "/api/users?rol=REVIEWER&texto=flujo-&soloActivos=false", "flujo-admin", "ADMIN").getBody();
        assertThat(todos.get("totalElementos").asInt()).isEqualTo(3);

        JsonNode yo = llamar(HttpMethod.GET, "/api/users/me", "flujo-marta", "REVIEWER,AUTHOR").getBody();
        assertThat(yo.get("sincronizado").asBoolean()).isTrue();
        assertThat(yo.get("correo").asText()).isEqualTo("flujo-marta@correo.test");

        // una segunda vuelta no crea nada nuevo y desactiva a quien Keycloak ya no trae
        when(keycloak.usuarios()).thenReturn(List.of(
                externo("flujo-admin", true, "ADMIN"), externo("flujo-ana", true, "AUTHOR"),
                externo("flujo-luis", true, "REVIEWER"), externo("flujo-pedro", false, "REVIEWER")));
        JsonNode segunda = llamar(HttpMethod.POST, "/api/users/sync", "flujo-admin", "ADMIN").getBody();
        assertThat(segunda.get("usuariosCreados").asInt()).isZero();
        assertThat(segunda.get("usuariosDesactivados").asInt()).isEqualTo(1);
        assertThat(llamar(HttpMethod.GET, "/api/users?rol=REVIEWER&texto=flujo-", "flujo-admin", "ADMIN").getBody()
                .get("contenido").findValuesAsText("id")).containsExactly("flujo-luis");
    }

    @Test
    @DisplayName("los inicios de sesión de Keycloak se publican como eventos auth.* y actualizan el último acceso")
    void iniciosDeSesion() {
        Instant ahora = Instant.now();
        when(keycloak.usuarios()).thenReturn(List.of(externo("sesion-ana", true, "AUTHOR")));
        when(keycloak.eventosDeSesion(any())).thenReturn(List.of(
                new EventoDeSesion(ahora.plusSeconds(1), true, "sesion-ana", "10.0.0.1", null),
                new EventoDeSesion(ahora.plusSeconds(2), false, "intruso", "10.0.0.9", "invalid_user_credentials")));
        llamar(HttpMethod.POST, "/api/users/sync", "admin-1", "ADMIN"); // sincroniza usuarios y sesiones
        clearInvocations(rabbit);
        when(keycloak.eventosDeSesion(any())).thenReturn(List.of(
                new EventoDeSesion(ahora.plusSeconds(30), true, "sesion-ana", "10.0.0.1", null)));

        ResponseEntity<JsonNode> segunda = llamar(HttpMethod.POST, "/api/users/sync", "admin-1", "ADMIN");

        assertThat(segunda.getBody().get("iniciosDeSesionPublicados").asInt()).isEqualTo(1);
        ArgumentCaptor<String> rutas = ArgumentCaptor.forClass(String.class);
        verify(rabbit, atLeast(1)).send(eq("bancopreguntas.events"), rutas.capture(), any(Message.class));
        assertThat(rutas.getAllValues()).containsExactly("auth.login-succeeded");
        assertThat(llamar(HttpMethod.GET, "/api/users/me", "sesion-ana", "AUTHOR").getBody().get("ultimoInicioDeSesion").asText())
                .isNotBlank();
    }

    @Test
    @DisplayName("un usuario no sincronizado todavía obtiene su identidad desde el token")
    void yoSinSincronizar() {
        JsonNode yo = llamar(HttpMethod.GET, "/api/users/me", "recien-llegado", "AUTHOR").getBody();

        assertThat(yo.get("id").asText()).isEqualTo("recien-llegado");
        assertThat(yo.get("sincronizado").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("si Keycloak no responde, sincronizar da 502 y el directorio se conserva")
    void keycloakCaido() {
        when(keycloak.usuarios()).thenThrow(new org.springframework.web.client.ResourceAccessException("sin conexión"));

        ResponseEntity<JsonNode> r = llamar(HttpMethod.POST, "/api/users/sync", "admin-1", "ADMIN");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(r.getBody().get("codigo").asText()).isEqualTo("PROVEEDOR_DE_IDENTIDAD_NO_DISPONIBLE");
    }

    @Test
    @DisplayName("los roles se exigen en cada operación y sin identidad responde 401")
    void roles() {
        assertThat(llamar(HttpMethod.GET, "/api/users", "autor-1", "AUTHOR").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.POST, "/api/users/sync", "rev-1", "REVIEWER").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(llamar(HttpMethod.GET, "/api/users/me", null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Swagger documenta el servicio")
    void swagger() {
        ResponseEntity<JsonNode> docs = http.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody().get("paths").has("/api/users/sync")).isTrue();
        assertThat(docs.getBody().get("info").get("title").asText()).isEqualTo("user-service");
    }
}
