package co.unicauca.bancopreguntas.assignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El servicio de revisión con la seguridad en modo {@code jwt}, como corre detrás del gateway con Keycloak:
 * el usuario y los roles salen del token firmado y las cabeceras de identidad no valen.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false",
                "bancopreguntas.security.mode=jwt"
        })
@Import({EmbeddedPostgresConfig.class, AssignmentServiceJwtTest.ClaveConfig.class})
@DisplayName("assignment-service con seguridad JWT")
class AssignmentServiceJwtTest {

    private static final RSAKey CLAVE = generar();

    private static RSAKey generar() {
        try {
            return new RSAKeyGenerator(2048).keyID("prueba").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @TestConfiguration
    static class ClaveConfig {

        @Bean
        JwtDecoder jwtDecoder() throws Exception {
            return NimbusJwtDecoder.withPublicKey(CLAVE.toRSAPublicKey()).build();
        }
    }

    @Autowired
    private TestRestTemplate http;

    @MockitoBean
    private RabbitTemplate rabbit;

    private static String token(String usuario, String rol) {
        try {
            JwtClaimsSet cuerpo = JwtClaimsSet.builder().subject("sub-" + usuario)
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                    .claim("preferred_username", usuario).claim("realm_access", Map.of("roles", List.of(rol))).build();
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(CLAVE)))
                    .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), cuerpo)).getTokenValue();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ResponseEntity<JsonNode> get(String ruta, String token, String... cabeceras) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) {
            h.setBearerAuth(token);
        }
        for (int i = 0; i < cabeceras.length; i += 2) {
            h.set(cabeceras[i], cabeceras[i + 1]);
        }
        return http.exchange(ruta, HttpMethod.GET, new HttpEntity<>(null, h), JsonNode.class);
    }

    @Test
    @DisplayName("sin token, 401 con el formato de error común")
    void sinToken() {
        ResponseEntity<JsonNode> r = get("/api/reviews/config", null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody().get("codigo").asText()).isEqualTo("NO_AUTENTICADO");
    }

    @Test
    @DisplayName("con token de administrador puede listar las preguntas por asignar")
    void administrador() {
        ResponseEntity<JsonNode> r = get("/api/reviews/pending-assignment", token("admin-1", "ADMIN"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().has("contenido")).isTrue();
    }

    @Test
    @DisplayName("con token de otro rol, 403")
    void otroRol() {
        assertThat(get("/api/reviews/pending-assignment", token("autor-1", "AUTHOR")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/reviews/assigned", token("admin-1", "ADMIN")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("las cabeceras de identidad no sirven para suplantar a nadie")
    void suplantacion() {
        assertThat(get("/api/reviews/pending-assignment", null, "X-User-Id", "admin-1", "X-User-Roles", "ADMIN")
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/reviews/pending-assignment", token("autor-1", "AUTHOR"), "X-User-Id", "admin-1",
                "X-User-Roles", "ADMIN").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("la salud y la documentación siguen siendo públicas")
    void publico() {
        assertThat(get("/actuator/health/liveness", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/v3/api-docs", null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
