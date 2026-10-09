package co.unicauca.bancopreguntas.platform.security;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resuelve el parámetro {@link AuthenticatedUser} a partir de las cabeceras {@code X-User-Id} y
 * {@code X-User-Roles}. El gateway las rellena tras validar el token; sin él (desarrollo y pruebas) se
 * envían a mano.
 */
public class HeaderUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER_USER = "X-User-Id";
    public static final String HEADER_ROLES = "X-User-Roles";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        String id = request.getHeader(HEADER_USER);
        if (id == null || id.isBlank()) {
            throw new UnauthenticatedException("Falta la identidad del usuario (cabecera " + HEADER_USER + ")");
        }
        return new AuthenticatedUser(id, AuthenticatedUser.rolesDe(request.getHeader(HEADER_ROLES)));
    }
}
