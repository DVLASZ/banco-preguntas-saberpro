package co.unicauca.bancopreguntas.platform.security;

import co.unicauca.bancopreguntas.platform.config.PlatformEventsAutoConfiguration;
import co.unicauca.bancopreguntas.platform.config.PlatformWebAutoConfiguration;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El modo {@code jwt} de la seguridad con tokens firmados de verdad (una clave RSA generada para la prueba):
 * comprueba la firma, la expiración y el emisor, la lectura de roles de Keycloak y que las cabeceras de
 * identidad no sirvan para suplantar a nadie.
 */
@SpringBootTest(properties = {
        "bancopreguntas.security.mode=jwt",
        "spring.main.banner-mode=off"
})
@AutoConfigureMockMvc
@DisplayName("Seguridad JWT (Keycloak)")
class JwtSecurityTest {

    private static final String EMISOR = "http://keycloak.test/realms/bancopreguntas";
    private static final RSAKey CLAVE = generar();
    private static final RSAKey OTRA_CLAVE = generar();

    private static RSAKey generar() {
        try {
            return new RSAKeyGenerator(2048).keyID(java.util.UUID.randomUUID().toString()).generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {RabbitAutoConfiguration.class, PlatformEventsAutoConfiguration.class,
            org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class})
    @Import({PlatformWebAutoConfiguration.class, PruebaController.class})
    static class App {

        @Bean
        JwtDecoder jwtDecoder() throws Exception {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(CLAVE.toRSAPublicKey()).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));
            return decoder;
        }
    }

    @RestController
    static class PruebaController {

        @GetMapping("/api/yo")
        Map<String, Object> yo(AuthenticatedUser usuario) {
            return Map.of("id", usuario.id(), "roles", usuario.roles());
        }

        @GetMapping("/api/solo-admin")
        String soloAdmin(AuthenticatedUser usuario) {
            usuario.requireAnyRole(Roles.ADMIN);
            return "ok";
        }

        @GetMapping("/actuator/health/ping")
        String salud() {
            return "UP";
        }
    }

    @Autowired
    private MockMvc mvc;

    private static String token(RSAKey clave, String emisor, Instant vence, Map<String, Object> reclamos) throws Exception {
        NimbusJwtEncoder codificador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
        JwtClaimsSet.Builder cuerpo = JwtClaimsSet.builder().issuer(emisor).subject("kc-uuid-123")
                .issuedAt(vence.minusSeconds(300)).expiresAt(vence);
        reclamos.forEach(cuerpo::claim);
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), cuerpo.build()))
                .getTokenValue();
    }

    private static String valido(String usuario, String... roles) throws Exception {
        return token(CLAVE, EMISOR, Instant.now().plusSeconds(300),
                Map.of("preferred_username", usuario, "realm_access", Map.of("roles", List.of(roles))));
    }

    private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder p, String token) {
        return p.header("Authorization", "Bearer " + token);
    }

    @Test
    @DisplayName("un token válido da la identidad y los roles del usuario")
    void tokenValido() throws Exception {
        mvc.perform(conToken(get("/api/yo"), valido("autor-1", "AUTHOR", "offline_access")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("autor-1"))
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.hasItems("AUTHOR", "OFFLINE_ACCESS")));
    }

    @Test
    @DisplayName("sin token responde 401 con el formato de error común")
    void sinToken() throws Exception {
        mvc.perform(get("/api/yo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("un token de otro rol recibe 403 al llamar una operación de administrador")
    void rolInsuficiente() throws Exception {
        mvc.perform(conToken(get("/api/solo-admin"), valido("autor-1", "AUTHOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(conToken(get("/api/solo-admin"), valido("admin-1", "ADMIN"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("un token vencido se rechaza")
    void vencido() throws Exception {
        String vencido = token(CLAVE, EMISOR, Instant.now().minusSeconds(120),
                Map.of("preferred_username", "autor-1", "realm_access", Map.of("roles", List.of("AUTHOR"))));

        mvc.perform(conToken(get("/api/yo"), vencido)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token de otro emisor se rechaza")
    void otroEmisor() throws Exception {
        String ajeno = token(CLAVE, "http://otro-emisor.test/realms/x", Instant.now().plusSeconds(300),
                Map.of("preferred_username", "autor-1", "realm_access", Map.of("roles", List.of("AUTHOR"))));

        mvc.perform(conToken(get("/api/yo"), ajeno)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token firmado con otra clave se rechaza")
    void firmaFalsa() throws Exception {
        String falso = token(OTRA_CLAVE, EMISOR, Instant.now().plusSeconds(300),
                Map.of("preferred_username", "admin-1", "realm_access", Map.of("roles", List.of("ADMIN"))));

        mvc.perform(conToken(get("/api/solo-admin"), falso)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un texto que no es un token se rechaza")
    void basura() throws Exception {
        mvc.perform(conToken(get("/api/yo"), "esto-no-es-un-jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("las cabeceras de identidad no sirven: sin token siguen siendo 401")
    void cabecerasSinToken() throws Exception {
        mvc.perform(get("/api/solo-admin").header("X-User-Id", "admin-1").header("X-User-Roles", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("con token, las cabeceras no cambian quién eres ni tus roles")
    void cabecerasConToken() throws Exception {
        mvc.perform(conToken(get("/api/solo-admin"), valido("autor-1", "AUTHOR"))
                        .header("X-User-Id", "admin-1").header("X-User-Roles", "ADMIN"))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/yo"), valido("autor-1", "AUTHOR")).header("X-User-Id", "admin-1"))
                .andExpect(jsonPath("$.id").value("autor-1"));
    }

    @Test
    @DisplayName("sin la claim preferred_username se usa el sujeto del token")
    void sinNombreDeUsuario() throws Exception {
        String sinNombre = token(CLAVE, EMISOR, Instant.now().plusSeconds(300),
                Map.of("realm_access", Map.of("roles", List.of("REVIEWER"))));

        mvc.perform(conToken(get("/api/yo"), sinNombre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("kc-uuid-123"));
    }

    @Test
    @DisplayName("un token sin roles, o con roles mal formados, no tiene permisos pero sí identidad")
    void sinRoles() throws Exception {
        String sinRoles = token(CLAVE, EMISOR, Instant.now().plusSeconds(300), Map.of("preferred_username", "u-1"));
        String malFormados = token(CLAVE, EMISOR, Instant.now().plusSeconds(300),
                Map.of("preferred_username", "u-2", "realm_access", Map.of("roles", "ADMIN")));

        mvc.perform(conToken(get("/api/yo"), sinRoles)).andExpect(status().isOk()).andExpect(jsonPath("$.roles").isEmpty());
        mvc.perform(conToken(get("/api/solo-admin"), malFormados)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("la salud del servicio es pública")
    void saludPublica() throws Exception {
        mvc.perform(get("/actuator/health/ping")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("el convertidor lee la claim configurada y los roles en mayúsculas")
    void convertidor() throws Exception {
        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("t")
                .header("alg", "none").claim("email", "a@b.c")
                .claim("realm_access", Map.of("roles", List.of("admin", "Reviewer"))).build();

        var autenticacion = new KeycloakJwtConverter("email").convert(jwt);

        org.assertj.core.api.Assertions.assertThat(autenticacion.getName()).isEqualTo("a@b.c");
        org.assertj.core.api.Assertions.assertThat(autenticacion.getAuthorities())
                .extracting(a -> a.getAuthority()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_REVIEWER");
    }
}
