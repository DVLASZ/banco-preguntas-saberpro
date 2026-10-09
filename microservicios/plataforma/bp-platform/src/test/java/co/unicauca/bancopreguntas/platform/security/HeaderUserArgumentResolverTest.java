package co.unicauca.bancopreguntas.platform.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HeaderUserArgumentResolver")
class HeaderUserArgumentResolverTest {

    private final HeaderUserArgumentResolver resolutor = new HeaderUserArgumentResolver();

    @SuppressWarnings("unused")
    private void metodo(AuthenticatedUser usuario, String otro) {
    }

    private MethodParameter parametro(int indice) throws NoSuchMethodException {
        return new MethodParameter(getClass().getDeclaredMethod("metodo", AuthenticatedUser.class, String.class), indice);
    }

    @Test
    @DisplayName("solo soporta parámetros de tipo AuthenticatedUser")
    void soporta() throws Exception {
        assertThat(resolutor.supportsParameter(parametro(0))).isTrue();
        assertThat(resolutor.supportsParameter(parametro(1))).isFalse();
    }

    @Test
    @DisplayName("construye el usuario con el identificador y los roles de las cabeceras")
    void resuelve() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-User-Id", "autor-1");
        peticion.addHeader("X-User-Roles", "author,reviewer");

        AuthenticatedUser usuario = (AuthenticatedUser) resolutor.resolveArgument(
                parametro(0), null, new ServletWebRequest(peticion), null);

        assertThat(usuario.id()).isEqualTo("autor-1");
        assertThat(usuario.roles()).containsExactlyInAnyOrder("AUTHOR", "REVIEWER");
    }

    @Test
    @DisplayName("sin cabecera de roles el usuario queda sin roles")
    void sinRoles() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-User-Id", "autor-1");

        AuthenticatedUser usuario = (AuthenticatedUser) resolutor.resolveArgument(
                parametro(0), null, new ServletWebRequest(peticion), null);

        assertThat(usuario.roles()).isEmpty();
    }

    @Test
    @DisplayName("sin identificador lanza UnauthenticatedException")
    void sinIdentidad() throws Exception {
        ServletWebRequest sinCabecera = new ServletWebRequest(new MockHttpServletRequest());
        MockHttpServletRequest enBlanco = new MockHttpServletRequest();
        enBlanco.addHeader("X-User-Id", "  ");

        assertThatThrownBy(() -> resolutor.resolveArgument(parametro(0), null, sinCabecera, null))
                .isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(() -> resolutor.resolveArgument(parametro(0), null, new ServletWebRequest(enBlanco), null))
                .isInstanceOf(UnauthenticatedException.class);
    }
}
