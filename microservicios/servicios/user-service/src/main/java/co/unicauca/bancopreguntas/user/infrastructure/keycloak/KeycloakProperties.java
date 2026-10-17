package co.unicauca.bancopreguntas.user.infrastructure.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Cómo hablar con Keycloak. El servicio entra con un cliente propio (credenciales de cliente) que solo tiene
 * permiso de lectura de usuarios y eventos.
 *
 * @param baseUrl      dirección de Keycloak vista desde este servicio (en Docker, {@code http://keycloak:8080})
 * @param realm        el realm del banco de preguntas
 * @param roles        los roles que definen a un usuario del banco; quien no tenga ninguno no se lista
 * @param maxUsuarios  tope de usuarios que se piden por rol
 */
@ConfigurationProperties(prefix = "user.keycloak")
public record KeycloakProperties(String baseUrl, String realm, String clientId, String clientSecret,
                                 List<String> roles, int maxUsuarios) {

    public KeycloakProperties {
        baseUrl = baseUrl == null ? "http://localhost:8180" : baseUrl.replaceAll("/+$", "");
        realm = realm == null || realm.isBlank() ? "bancopreguntas" : realm;
        roles = roles == null || roles.isEmpty() ? List.of("ADMIN", "AUTHOR", "REVIEWER") : List.copyOf(roles);
        maxUsuarios = maxUsuarios <= 0 ? 2000 : maxUsuarios;
    }
}
