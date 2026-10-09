package co.unicauca.bancopreguntas.platform.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convierte el token JWT de Keycloak en la autenticación de Spring Security: el nombre del usuario sale de la
 * claim configurada (por defecto {@code preferred_username}) y los roles, de {@code realm_access.roles}, como
 * autoridades {@code ROLE_<ROL>} en mayúsculas.
 */
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String CLAIM_POR_DEFECTO = "preferred_username";

    private final String claimDelUsuario;

    public KeycloakJwtConverter(String claimDelUsuario) {
        this.claimDelUsuario = claimDelUsuario == null || claimDelUsuario.isBlank() ? CLAIM_POR_DEFECTO : claimDelUsuario;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String usuario = jwt.getClaimAsString(claimDelUsuario);
        if (usuario == null || usuario.isBlank()) {
            usuario = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, autoridades(jwt), usuario);
    }

    private static Collection<GrantedAuthority> autoridades(Jwt jwt) {
        Object acceso = jwt.getClaim("realm_access");
        if (!(acceso instanceof Map<?, ?> mapa) || !(mapa.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + ((String) r).trim().toUpperCase(Locale.ROOT)))
                .toList();
    }
}
