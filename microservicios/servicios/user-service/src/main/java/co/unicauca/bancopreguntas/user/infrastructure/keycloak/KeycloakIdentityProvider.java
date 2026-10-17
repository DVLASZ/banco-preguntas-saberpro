package co.unicauca.bancopreguntas.user.infrastructure.keycloak;

import co.unicauca.bancopreguntas.user.application.port.IdentityProvider;
import co.unicauca.bancopreguntas.user.domain.EventoDeSesion;
import co.unicauca.bancopreguntas.user.domain.UsuarioExterno;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Adaptador del puerto {@link IdentityProvider} sobre la API de administración de Keycloak. Los usuarios del
 * banco son los miembros de los roles ADMIN, AUTHOR y REVIEWER; los inicios de sesión se leen de los eventos
 * LOGIN y LOGIN_ERROR que Keycloak guarda.
 */
public class KeycloakIdentityProvider implements IdentityProvider {

    private static final long MARGEN_DEL_TOKEN_SEGUNDOS = 20;

    private final RestClient http;
    private final KeycloakProperties propiedades;
    private final Clock reloj;

    private String token;
    private Instant tokenVenceEn = Instant.MIN;

    public KeycloakIdentityProvider(RestClient http, KeycloakProperties propiedades, Clock reloj) {
        this.http = http;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ usuarios

    @Override
    public List<UsuarioExterno> usuarios() {
        Map<String, Miembro> porUsuario = new LinkedHashMap<>();
        for (String rol : propiedades.roles()) {
            JsonNode miembros = administracion("/roles/{rol}/users?max={max}", rol, propiedades.maxUsuarios());
            for (JsonNode m : miembros) {
                String username = m.path("username").asText("");
                if (username.isBlank()) {
                    continue;
                }
                Miembro miembro = porUsuario.computeIfAbsent(username, u -> new Miembro(m));
                miembro.roles.add(rol.toUpperCase());
            }
        }
        return porUsuario.values().stream()
                .map(Miembro::aUsuario)
                .sorted(Comparator.comparing(UsuarioExterno::username))
                .toList();
    }

    private static final class Miembro {
        final JsonNode datos;
        final Set<String> roles = new TreeSet<>();

        Miembro(JsonNode datos) {
            this.datos = datos;
        }

        UsuarioExterno aUsuario() {
            String nombre = (datos.path("firstName").asText("") + " " + datos.path("lastName").asText("")).trim();
            String correo = datos.path("email").asText("");
            return new UsuarioExterno(datos.path("id").asText(null), datos.path("username").asText(),
                    nombre.isBlank() ? null : nombre, correo.isBlank() ? null : correo, roles,
                    datos.path("enabled").asBoolean(true));
        }
    }

    // ------------------------------------------------------------------ inicios de sesión

    @Override
    public List<EventoDeSesion> eventosDeSesion(Instant desde) {
        String dia = desde.atZone(ZoneOffset.UTC).toLocalDate().toString();
        JsonNode eventos = administracion("/events?type=LOGIN&type=LOGIN_ERROR&dateFrom={dia}&max=1000", dia);
        List<EventoDeSesion> resultado = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (JsonNode e : eventos) {
            Instant momento = Instant.ofEpochMilli(e.path("time").asLong());
            String usuario = e.path("details").path("username").asText("");
            if (!momento.isAfter(desde) || usuario.isBlank()) {
                continue;
            }
            boolean exitoso = "LOGIN".equals(e.path("type").asText());
            String ip = e.path("ipAddress").asText(null);
            String clave = momento + "|" + usuario + "|" + exitoso + "|" + ip;
            if (!vistos.add(clave)) {
                continue;
            }
            resultado.add(new EventoDeSesion(momento, exitoso, usuario, ip, exitoso ? null : e.path("error").asText(null)));
        }
        resultado.sort(Comparator.comparing(EventoDeSesion::momento));
        return resultado;
    }

    // ------------------------------------------------------------------ acceso a Keycloak

    private JsonNode administracion(String ruta, Object... variables) {
        String url = propiedades.baseUrl() + "/admin/realms/" + propiedades.realm() + ruta;
        return http.get().uri(url, variables)
                .header("Authorization", "Bearer " + tokenDeAcceso())
                .retrieve()
                .body(JsonNode.class);
    }

    private synchronized String tokenDeAcceso() {
        Instant ahora = reloj.instant();
        if (token != null && ahora.isBefore(tokenVenceEn)) {
            return token;
        }
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "client_credentials");
        formulario.add("client_id", propiedades.clientId());
        formulario.add("client_secret", propiedades.clientSecret());
        JsonNode respuesta = http.post()
                .uri(propiedades.baseUrl() + "/realms/" + propiedades.realm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(JsonNode.class);
        if (respuesta == null || !respuesta.hasNonNull("access_token")) {
            throw new IllegalStateException("Keycloak no entregó un token de acceso");
        }
        token = respuesta.get("access_token").asText();
        tokenVenceEn = ahora.plusSeconds(Math.max(0, respuesta.path("expires_in").asLong(60) - MARGEN_DEL_TOKEN_SEGUNDOS));
        return token;
    }
}
