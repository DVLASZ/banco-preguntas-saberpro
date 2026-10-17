package co.unicauca.bancopreguntas.user.infrastructure.keycloak;

import co.unicauca.bancopreguntas.user.domain.EventoDeSesion;
import co.unicauca.bancopreguntas.user.domain.UsuarioExterno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("KeycloakIdentityProvider (API de administración)")
class KeycloakIdentityProviderTest {

    private static final String BASE = "http://keycloak.test:8080";
    private static final String ADMIN = BASE + "/admin/realms/bancopreguntas";
    private static final String TOKEN = BASE + "/realms/bancopreguntas/protocol/openid-connect/token";
    private static final Instant AHORA = Instant.parse("2026-10-08T12:00:00Z");

    private MockRestServiceServer servidor;
    private KeycloakIdentityProvider keycloak;
    private final MutableClock reloj = new MutableClock(AHORA);

    /** Reloj que la prueba puede adelantar. */
    private static final class MutableClock extends Clock {
        private Instant ahora;

        MutableClock(Instant ahora) {
            this.ahora = ahora;
        }

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        keycloak = new KeycloakIdentityProvider(builder.build(),
                new KeycloakProperties(BASE + "/", "bancopreguntas", "bancopreguntas-admin", "secreto-de-prueba",
                        List.of("ADMIN", "AUTHOR", "REVIEWER"), 500), reloj);
    }

    private void token(String valor, int expiraEn) {
        servidor.expect(once(), requestTo(TOKEN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=client_credentials")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_id=bancopreguntas-admin")))
                .andRespond(withSuccess("{\"access_token\":\"" + valor + "\",\"expires_in\":" + expiraEn + "}",
                        MediaType.APPLICATION_JSON));
    }

    private void miembros(String rol, String json, String token) {
        servidor.expect(once(), requestTo(ADMIN + "/roles/" + rol + "/users?max=500"))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("los usuarios son los miembros de los roles, con todos sus roles reunidos")
    void usuarios() {
        token("t1", 300);
        miembros("ADMIN", "[{\"id\":\"k1\",\"username\":\"admin\",\"firstName\":\"Ada\",\"lastName\":\"Admin\","
                + "\"email\":\"admin@x.co\",\"enabled\":true}]", "t1");
        miembros("AUTHOR", "[{\"id\":\"k2\",\"username\":\"luis\",\"firstName\":\"Luis\",\"enabled\":true},"
                + "{\"id\":\"k1\",\"username\":\"admin\",\"enabled\":true}]", "t1");
        miembros("REVIEWER", "[{\"id\":\"k2\",\"username\":\"luis\",\"firstName\":\"Luis\",\"enabled\":true},"
                + "{\"id\":\"k3\",\"username\":\"marta\",\"enabled\":false}]", "t1");

        List<UsuarioExterno> usuarios = keycloak.usuarios();

        assertThat(usuarios).extracting(UsuarioExterno::username).containsExactly("admin", "luis", "marta");
        UsuarioExterno admin = usuarios.get(0);
        assertThat(admin.nombre()).isEqualTo("Ada Admin");
        assertThat(admin.correo()).isEqualTo("admin@x.co");
        assertThat(admin.roles()).containsExactlyInAnyOrder("ADMIN", "AUTHOR");
        assertThat(usuarios.get(1).roles()).containsExactlyInAnyOrder("AUTHOR", "REVIEWER");
        assertThat(usuarios.get(1).nombre()).isEqualTo("Luis");
        assertThat(usuarios.get(1).correo()).isNull();
        assertThat(usuarios.get(2).activo()).isFalse();
        assertThat(usuarios.get(2).nombre()).isNull();
        servidor.verify();
    }

    @Test
    @DisplayName("reutiliza el token mientras no venza y pide otro cuando vence")
    void tokenReutilizado() {
        servidor.expect(once(), requestTo(TOKEN)).andRespond(withSuccess(
                "{\"access_token\":\"t1\",\"expires_in\":300}", MediaType.APPLICATION_JSON));
        miembros("ADMIN", "[]", "t1");
        miembros("AUTHOR", "[]", "t1");
        miembros("REVIEWER", "[]", "t1");
        keycloak.usuarios();
        servidor.verify();
        servidor.reset();

        // sin pedir otro token
        miembros("ADMIN", "[]", "t1");
        miembros("AUTHOR", "[]", "t1");
        miembros("REVIEWER", "[]", "t1");
        reloj.avanzar(Duration.ofSeconds(200));
        keycloak.usuarios();
        servidor.verify();
        servidor.reset();

        // el token vence (300 s menos el margen): pide uno nuevo
        reloj.avanzar(Duration.ofSeconds(150));
        token("t2", 300);
        miembros("ADMIN", "[]", "t2");
        miembros("AUTHOR", "[]", "t2");
        miembros("REVIEWER", "[]", "t2");
        keycloak.usuarios();
        servidor.verify();
    }

    @Test
    @DisplayName("los inicios de sesión salen de los eventos LOGIN y LOGIN_ERROR, solo los posteriores al corte")
    void eventos() {
        token("t1", 300);
        long antes = AHORA.minusSeconds(60).toEpochMilli();
        long despues1 = AHORA.plusSeconds(10).toEpochMilli();
        long despues2 = AHORA.plusSeconds(20).toEpochMilli();
        servidor.expect(once(), requestTo(ADMIN + "/events?type=LOGIN&type=LOGIN_ERROR&dateFrom=2026-10-08&max=1000"))
                .andExpect(header("Authorization", "Bearer t1"))
                .andRespond(withSuccess("""
                        [{"time":%d,"type":"LOGIN_ERROR","ipAddress":"10.0.0.9","error":"invalid_user_credentials","details":{"username":"intruso"}},
                         {"time":%d,"type":"LOGIN","userId":"k1","ipAddress":"10.0.0.1","details":{"username":"ana"}},
                         {"time":%d,"type":"LOGIN","userId":"k9","details":{"username":"viejo"}},
                         {"time":%d,"type":"LOGIN","userId":"k1","ipAddress":"10.0.0.1","details":{"username":"ana"}},
                         {"time":%d,"type":"LOGIN_ERROR","error":"user_not_found","details":{}}]
                        """.formatted(despues2, despues1, antes, despues1, despues2), MediaType.APPLICATION_JSON));

        List<EventoDeSesion> eventos = keycloak.eventosDeSesion(AHORA);

        assertThat(eventos).hasSize(2); // el viejo, el repetido y el que no trae usuario se descartan
        assertThat(eventos.get(0)).isEqualTo(new EventoDeSesion(AHORA.plusSeconds(10), true, "ana", "10.0.0.1", null));
        assertThat(eventos.get(1)).isEqualTo(
                new EventoDeSesion(AHORA.plusSeconds(20), false, "intruso", "10.0.0.9", "invalid_user_credentials"));
    }

    @Test
    @DisplayName("si Keycloak rechaza las credenciales del servicio, la consulta falla con claridad")
    void credencialesRechazadas() {
        servidor.expect(once(), requestTo(TOKEN)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> keycloak.usuarios()).isInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("si Keycloak no entrega token, falla")
    void sinToken() {
        servidor.expect(once(), requestTo(TOKEN)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> keycloak.usuarios()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("token");
    }

    @Test
    @DisplayName("un error de la API de administración se propaga")
    void errorDeLaApi() {
        token("t1", 300);
        servidor.expect(times(1), requestTo(ADMIN + "/roles/ADMIN/users?max=500"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> keycloak.usuarios()).isInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("las propiedades tienen valores por defecto razonables")
    void propiedades() {
        KeycloakProperties p = new KeycloakProperties(null, null, "c", "s", null, 0);

        assertThat(p.baseUrl()).isEqualTo("http://localhost:8180");
        assertThat(p.realm()).isEqualTo("bancopreguntas");
        assertThat(p.roles()).containsExactly("ADMIN", "AUTHOR", "REVIEWER");
        assertThat(p.maxUsuarios()).isEqualTo(2000);
        assertThat(new KeycloakProperties("http://kc:8080//", "r", "c", "s", List.of("X"), 5).baseUrl())
                .isEqualTo("http://kc:8080");
    }
}
