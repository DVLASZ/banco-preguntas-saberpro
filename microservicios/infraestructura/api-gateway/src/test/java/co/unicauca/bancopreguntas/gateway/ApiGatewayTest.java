package co.unicauca.bancopreguntas.gateway;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El gateway completo contra un servicio simulado (un servidor HTTP local): rutas, validación del token con
 * claves reales, roles por ruta, CORS, identificador de petición, reintentos y respuesta cuando un servicio
 * no responde.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"gateway.timeout=3s", "gateway.cors.allowed-origins=http://localhost:4200"})
@AutoConfigureWebTestClient
@Import(ApiGatewayTest.ClaveConfig.class)
@DisplayName("api-gateway")
class ApiGatewayTest {

    private record Recibida(String metodo, String camino, HttpHeaders cabeceras) {
    }

    private static final RSAKey CLAVE = generar();
    private static final List<Recibida> RECIBIDAS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger LLAMADAS_INESTABLES = new AtomicInteger();
    private static HttpServer servicio;

    private static RSAKey generar() {
        try {
            return new RSAKeyGenerator(2048).keyID("prueba").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Servicio simulado: responde 200 a todo, salvo /api/reviews/inestable, que falla la primera vez. */
    private static synchronized HttpServer iniciarServicio() {
        if (servicio != null) {
            return servicio;
        }
        try {
            servicio = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servicio.createContext("/", intercambio -> {
                HttpHeaders cabeceras = new HttpHeaders();
                intercambio.getRequestHeaders().forEach((k, v) -> cabeceras.put(k, v));
                String camino = intercambio.getRequestURI().getPath();
                RECIBIDAS.add(new Recibida(intercambio.getRequestMethod(), camino, cabeceras));
                boolean falla = camino.equals("/api/reviews/inestable") && LLAMADAS_INESTABLES.incrementAndGet() == 1;
                byte[] cuerpo = (falla ? "{\"codigo\":\"CAIDO\"}" : "{\"ok\":true,\"camino\":\"" + camino + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().set("Content-Type", "application/json");
                intercambio.sendResponseHeaders(falla ? 503 : 200, cuerpo.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(cuerpo);
                }
            });
            servicio.start();
            return servicio;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void rutas(DynamicPropertyRegistry registro) {
        String destino = "http://127.0.0.1:" + iniciarServicio().getAddress().getPort();
        registro.add("gateway.uris.question-service", () -> destino);
        registro.add("gateway.uris.review-service", () -> destino);
        registro.add("gateway.uris.notification-service", () -> destino);
        registro.add("gateway.uris.history-service", () -> destino);
        registro.add("gateway.uris.simulation-service", () -> destino);
        registro.add("gateway.uris.tracking-service", () -> destino);
        registro.add("gateway.uris.user-service", () -> "http://127.0.0.1:1"); // nadie escucha: el servicio está caído
    }

    @AfterAll
    static void detener() {
        if (servicio != null) {
            servicio.stop(0);
        }
    }

    @TestConfiguration
    static class ClaveConfig {

        @Bean
        @Primary
        ReactiveJwtDecoder decodificadorDePrueba() throws Exception {
            return NimbusReactiveJwtDecoder.withPublicKey(CLAVE.toRSAPublicKey()).build();
        }
    }

    @Autowired
    private WebTestClient cliente;

    @org.springframework.boot.test.web.server.LocalServerPort
    private int puerto;

    @BeforeEach
    void limpiar() {
        RECIBIDAS.clear();
        LLAMADAS_INESTABLES.set(0);
    }

    private static String token(String usuario, String... roles) {
        try {
            JwtClaimsSet cuerpo = JwtClaimsSet.builder().subject("sub-" + usuario)
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                    .claim("preferred_username", usuario).claim("realm_access", Map.of("roles", List.of(roles))).build();
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(CLAVE)))
                    .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), cuerpo)).getTokenValue();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- autenticación

    @Test
    @DisplayName("sin token, 401 con el formato de error común")
    void sinToken() {
        cliente.get().uri("/api/reviews/config").exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTENTICADO");
        assertThat(RECIBIDAS).isEmpty();
    }

    @Test
    @DisplayName("con un token de otra clave, 401")
    void tokenFalso() throws Exception {
        RSAKey otra = new RSAKeyGenerator(2048).keyID("otra").generate();
        JwtClaimsSet cuerpo = JwtClaimsSet.builder().subject("x").expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of("ADMIN"))).build();
        String falso = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(otra)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), cuerpo)).getTokenValue();

        cliente.get().uri("/api/reviews/config").header("Authorization", "Bearer " + falso).exchange()
                .expectStatus().isUnauthorized();
        assertThat(RECIBIDAS).isEmpty();
    }

    @Test
    @DisplayName("con token válido la petición llega al servicio con su ruta y el token")
    void reenvia() {
        String token = token("autor-1", "AUTHOR");

        cliente.get().uri("/api/reviews/config").header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.ok").isEqualTo(true).jsonPath("$.camino").isEqualTo("/api/reviews/config");

        assertThat(RECIBIDAS).singleElement().satisfies(r -> {
            assertThat(r.camino()).isEqualTo("/api/reviews/config");
            assertThat(r.cabeceras().getFirst("Authorization")).isEqualTo("Bearer " + token);
        });
    }

    @Test
    @DisplayName("cada servicio se alcanza por su ruta")
    void rutas() {
        String admin = token("admin-1", "ADMIN", "AUTHOR", "REVIEWER");
        for (String camino : List.of("/api/questions/all", "/api/reviews/config", "/api/notifications", "/api/history/me")) {
            cliente.get().uri(camino).header("Authorization", "Bearer " + admin).exchange().expectStatus().isOk();
        }

        assertThat(RECIBIDAS).extracting(Recibida::camino)
                .containsExactlyInAnyOrder("/api/questions/all", "/api/reviews/config", "/api/notifications", "/api/history/me");
    }

    @Test
    @DisplayName("las cabeceras de identidad que envía el cliente no llegan al servicio")
    void descartaIdentidad() {
        cliente.get().uri("/api/reviews/config").header("Authorization", "Bearer " + token("autor-1", "AUTHOR"))
                .header("X-User-Id", "admin-1").header("X-User-Roles", "ADMIN").exchange().expectStatus().isOk();

        assertThat(RECIBIDAS).singleElement().satisfies(r -> {
            assertThat(r.cabeceras().containsKey("X-User-Id")).isFalse();
            assertThat(r.cabeceras().containsKey("X-User-Roles")).isFalse();
        });
    }

    // ---------------------------------------------------------------- roles

    @Test
    @DisplayName("un autor no pasa a las operaciones del administrador, que ni llegan al servicio")
    void rolesDeAdministrador() {
        String autor = "Bearer " + token("autor-1", "AUTHOR");

        cliente.get().uri("/api/reviews/pending-assignment").header("Authorization", autor).exchange()
                .expectStatus().isForbidden().expectBody().jsonPath("$.codigo").isEqualTo("ACCESO_DENEGADO");
        cliente.post().uri("/api/reviews/q-1/assignments").header("Authorization", autor).exchange().expectStatus().isForbidden();
        cliente.post().uri("/api/questions/q-1/publish").header("Authorization", autor).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/questions/all").header("Authorization", autor).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/history/audit").header("Authorization", autor).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/notifications/emails").header("Authorization", autor).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/users").header("Authorization", autor).exchange().expectStatus().isForbidden();
        assertThat(RECIBIDAS).isEmpty();
    }

    @Test
    @DisplayName("el administrador sí pasa a sus operaciones")
    void administrador() {
        String admin = "Bearer " + token("admin-1", "ADMIN");

        cliente.get().uri("/api/reviews/pending-assignment").header("Authorization", admin).exchange().expectStatus().isOk();
        cliente.post().uri("/api/reviews/q-1/assignments").header("Authorization", admin).exchange().expectStatus().isOk();
        cliente.post().uri("/api/questions/q-1/publish").header("Authorization", admin).exchange().expectStatus().isOk();
        cliente.get().uri("/api/history/audit").header("Authorization", admin).exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("solo el revisor evalúa y consulta su bandeja; solo el autor crea preguntas")
    void revisoresYAutores() {
        String revisor = "Bearer " + token("rev-1", "REVIEWER");
        String autor = "Bearer " + token("autor-1", "AUTHOR");

        cliente.post().uri("/api/reviews/q-1/evaluations").header("Authorization", revisor).exchange().expectStatus().isOk();
        cliente.get().uri("/api/reviews/assigned").header("Authorization", revisor).exchange().expectStatus().isOk();
        cliente.post().uri("/api/reviews/q-1/evaluations").header("Authorization", autor).exchange().expectStatus().isForbidden();
        cliente.post().uri("/api/questions").header("Authorization", autor).exchange().expectStatus().isOk();
        cliente.post().uri("/api/questions").header("Authorization", revisor).exchange().expectStatus().isForbidden();
        cliente.put().uri("/api/questions/q-1").header("Authorization", revisor).exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("un usuario con un rol ajeno al banco no pasa a ninguna ruta de la API")
    void rolAjeno() {
        String ajeno = "Bearer " + token("raro-1", "VISITANTE");

        cliente.get().uri("/api/questions/q-1").header("Authorization", ajeno).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/users/me").header("Authorization", ajeno).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/notifications").header("Authorization", ajeno).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/simulations").header("Authorization", ajeno).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/tracking/me/stats").header("Authorization", ajeno).exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("el estudiante solo llega a sus simulacros, su seguimiento y su perfil; no al banco de preguntas")
    void estudiante() {
        String estudiante = "Bearer " + token("est-1", "STUDENT");

        for (String camino : List.of("/api/simulations", "/api/simulations/catalog/summary",
                "/api/simulations/s-1", "/api/tracking/me/stats", "/api/tracking/me/history", "/api/tracking/me/competencies")) {
            cliente.get().uri(camino).header("Authorization", estudiante).exchange().expectStatus().isOk();
        }
        // el perfil sí pasa la autorización; responde 503 porque en esta prueba user-service está caído
        cliente.get().uri("/api/users/me").header("Authorization", estudiante).exchange().expectStatus().isEqualTo(503);
        cliente.post().uri("/api/simulations").header("Authorization", estudiante).exchange().expectStatus().isOk();
        cliente.post().uri("/api/simulations/s-1/answers").header("Authorization", estudiante).exchange().expectStatus().isOk();
        cliente.post().uri("/api/simulations/s-1/finish").header("Authorization", estudiante).exchange().expectStatus().isOk();

        for (String camino : List.of("/api/questions/q-1", "/api/reviews/config", "/api/notifications", "/api/history/me",
                "/api/tracking/reports/group", "/api/tracking/students/est-2/stats", "/api/users")) {
            cliente.get().uri(camino).header("Authorization", estudiante).exchange().expectStatus().isForbidden();
        }
    }

    @Test
    @DisplayName("el docente consulta simulacros, el seguimiento de cualquier estudiante y los reportes, pero no genera simulacros")
    void docente() {
        String docente = "Bearer " + token("doc-1", "TEACHER");

        for (String camino : List.of("/api/simulations/s-1", "/api/simulations/catalog/summary",
                "/api/tracking/students/est-2/stats", "/api/tracking/reports/group", "/api/tracking/reports/group.csv")) {
            cliente.get().uri(camino).header("Authorization", docente).exchange().expectStatus().isOk();
        }
        cliente.get().uri("/api/users/me").header("Authorization", docente).exchange().expectStatus().isEqualTo(503);
        cliente.post().uri("/api/simulations").header("Authorization", docente).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/tracking/me/stats").header("Authorization", docente).exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/questions/all").header("Authorization", docente).exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("el administrador también ve simulacros y reportes de seguimiento")
    void administradorVeSimulacrosYSeguimiento() {
        String admin = "Bearer " + token("admin-1", "ADMIN");

        for (String camino : List.of("/api/simulations/s-1", "/api/tracking/reports/group", "/api/tracking/students/est-2/history")) {
            cliente.get().uri(camino).header("Authorization", admin).exchange().expectStatus().isOk();
        }
        cliente.post().uri("/api/simulations").header("Authorization", admin).exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("un usuario con un token válido pero sin roles queda fuera")
    void sinRoles() {
        cliente.get().uri("/api/reviews/config").header("Authorization", "Bearer " + token("nadie")).exchange()
                .expectStatus().isForbidden();
    }

    // ---------------------------------------------------------------- público

    @Test
    @DisplayName("la salud y los documentos Swagger de cada servicio son públicos")
    void publico() {
        cliente.get().uri("/actuator/health").exchange().expectStatus().isOk();
        cliente.get().uri("/docs/question-service/v3/api-docs").exchange().expectStatus().isOk();
        cliente.get().uri("/docs/review-service/v3/api-docs").exchange().expectStatus().isOk();

        assertThat(RECIBIDAS).extracting(Recibida::camino).containsOnly("/v3/api-docs");
    }

    @Test
    @DisplayName("la interfaz de Swagger del gateway se sirve sin token")
    void swagger() {
        cliente.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
    }

    // ---------------------------------------------------------------- CORS e identificador

    @Test
    @DisplayName("el navegador de la aplicación web puede hacer la consulta previa (CORS)")
    void corsPermitido() throws Exception {
        // Se usa un cliente HTTP real: es lo que hace el navegador, con el Host del servidor y el Origin de la web.
        java.net.http.HttpRequest consulta = java.net.http.HttpRequest.newBuilder(
                        java.net.URI.create("http://localhost:" + puerto + "/api/questions"))
                .method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .build();

        java.net.http.HttpResponse<String> respuesta = java.net.http.HttpClient.newHttpClient()
                .send(consulta, java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:4200");
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Methods").orElse("")).contains("POST");
        assertThat(respuesta.headers().firstValue("Access-Control-Max-Age")).contains("3600");
        assertThat(RECIBIDAS).isEmpty();
    }

    @Test
    @DisplayName("un origen desconocido no pasa la consulta previa")
    void corsNoPermitido() {
        cliente.options().uri("/api/questions")
                .header("Origin", "http://sitio-malicioso.test")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("cada petición lleva un identificador que llega al servicio y vuelve en la respuesta")
    void identificador() {
        String token = "Bearer " + token("autor-1", "AUTHOR");

        String generado = cliente.get().uri("/api/reviews/config").header("Authorization", token).exchange()
                .expectStatus().isOk().returnResult(String.class).getResponseHeaders().getFirst("X-Request-Id");
        cliente.get().uri("/api/reviews/config").header("Authorization", token).header("X-Request-Id", "mi-traza-1")
                .exchange().expectHeader().valueEquals("X-Request-Id", "mi-traza-1");
        cliente.get().uri("/api/reviews/config").header("Authorization", token).header("X-Request-Id", "no válido <script>")
                .exchange().expectHeader().value("X-Request-Id", v -> assertThat(v).doesNotContain("script"));

        assertThat(generado).isNotBlank();
        assertThat(RECIBIDAS.get(0).cabeceras().getFirst("X-Request-Id")).isEqualTo(generado);
        assertThat(RECIBIDAS.get(1).cabeceras().getFirst("X-Request-Id")).isEqualTo("mi-traza-1");
    }

    // ---------------------------------------------------------------- resiliencia

    @Test
    @DisplayName("una consulta GET que falla en una réplica se reintenta y el cliente no lo nota")
    void reintento() {
        cliente.get().uri("/api/reviews/inestable").header("Authorization", "Bearer " + token("admin-1", "ADMIN")).exchange()
                .expectStatus().isOk();

        assertThat(RECIBIDAS).extracting(Recibida::camino).containsExactly("/api/reviews/inestable", "/api/reviews/inestable");
    }

    @Test
    @DisplayName("un POST que falla no se reintenta, para no repetir una acción")
    void sinReintentoEnPost() {
        cliente.post().uri("/api/reviews/inestable").header("Authorization", "Bearer " + token("admin-1", "ADMIN")).exchange()
                .expectStatus().isEqualTo(503);

        assertThat(RECIBIDAS).hasSize(1);
    }

    @Test
    @DisplayName("si un servicio está caído, 503 inmediato con el formato de error común")
    void servicioCaido() {
        cliente.get().uri("/api/users/me").header("Authorization", "Bearer " + token("autor-1", "AUTHOR")).exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("SERVICIO_NO_DISPONIBLE")
                .jsonPath("$.mensaje").value(m -> assertThat((String) m).contains("user-service"));
    }

    @Test
    @DisplayName("la caída de un servicio no afecta a los demás")
    void aislamiento() {
        String token = "Bearer " + token("admin-1", "ADMIN");

        cliente.get().uri("/api/users").header("Authorization", token).exchange().expectStatus().isEqualTo(503);
        cliente.get().uri("/api/reviews/config").header("Authorization", token).exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("el mensaje de respaldo es público y no depende de ningún servicio")
    void respaldo() {
        cliente.method(HttpMethod.GET).uri("/fallback/review-service").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.codigo").isEqualTo("SERVICIO_NO_DISPONIBLE");
    }

    @Test
    @DisplayName("una ruta que no existe responde 404 a quien tiene token")
    void rutaDesconocida() {
        cliente.get().uri("/api/desconocida").header("Authorization", "Bearer " + token("admin-1", "ADMIN")).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("las reglas de rol cubren todas las rutas de la API")
    void reglas() {
        assertThat(GatewayProperties.SERVICIOS.values().stream().flatMap(List::stream))
                .allSatisfy(camino -> assertThat(camino).startsWith("/api/"));
        assertThat(co.unicauca.bancopreguntas.gateway.security.RoleRules.REGLAS).isNotEmpty()
                .allSatisfy(r -> assertThat(r.roles()).isNotEmpty());
    }
}
