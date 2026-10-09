package co.unicauca.bancopreguntas.platform.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.stream.Collectors;

/**
 * Resuelve el parámetro {@link AuthenticatedUser} a partir del token JWT ya validado por Spring Security.
 * En este modo se ignoran las cabeceras {@code X-User-Id} y {@code X-User-Roles}: nadie puede hacerse pasar
 * por otro usuario enviándolas.
 */
public class JwtUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PREFIJO_ROL = "ROLE_";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacion instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            throw new UnauthenticatedException("Falta el token de autenticación");
        }
        return new AuthenticatedUser(token.getName(), token.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith(PREFIJO_ROL))
                .map(a -> a.substring(PREFIJO_ROL.length()))
                .collect(Collectors.toSet()));
    }
}
