package co.unicauca.bancopreguntas.platform.client;

import co.unicauca.bancopreguntas.platform.config.PlatformClientAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Propagación de la identidad en las llamadas entre servicios")
class IdentityPropagationInterceptorTest {

    private final IdentityPropagationInterceptor interceptor = new IdentityPropagationInterceptor();

    @AfterEach
    void limpiar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private MockClientHttpRequest salida() {
        return new MockClientHttpRequest(HttpMethod.POST, URI.create("http://otro-servicio/api/x"));
    }

    private void ejecutar(MockClientHttpRequest peticion) throws Exception {
        ClientHttpRequestExecution ejecucion = (r, b) -> mock(ClientHttpResponse.class);
        interceptor.intercept(peticion, new byte[0], ejecucion);
    }

    @Test
    @DisplayName("copia el token y las cabeceras de identidad de la petición que se está atendiendo")
    void copiaLaIdentidad() throws Exception {
        MockHttpServletRequest entrada = new MockHttpServletRequest();
        entrada.addHeader("Authorization", "Bearer abc");
        entrada.addHeader("X-User-Id", "autor1");
        entrada.addHeader("X-User-Roles", "AUTHOR");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(entrada));
        MockClientHttpRequest peticion = salida();

        ejecutar(peticion);

        assertThat(peticion.getHeaders().getFirst("Authorization")).isEqualTo("Bearer abc");
        assertThat(peticion.getHeaders().getFirst("X-User-Id")).isEqualTo("autor1");
        assertThat(peticion.getHeaders().getFirst("X-User-Roles")).isEqualTo("AUTHOR");
    }

    @Test
    @DisplayName("solo copia lo que la petición trae y no pisa lo que el cliente ya puso")
    void noPisaNiInventa() throws Exception {
        MockHttpServletRequest entrada = new MockHttpServletRequest();
        entrada.addHeader("X-User-Id", "autor1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(entrada));
        MockClientHttpRequest peticion = salida();
        peticion.getHeaders().set("X-User-Id", "otro");

        ejecutar(peticion);

        assertThat(peticion.getHeaders().getFirst("X-User-Id")).isEqualTo("otro");
        assertThat(peticion.getHeaders().containsKey("Authorization")).isFalse();
        assertThat(peticion.getHeaders().containsKey("X-User-Roles")).isFalse();
    }

    @Test
    @DisplayName("no manda el token a un servidor externo")
    void noLoMandaAfuera() throws Exception {
        MockHttpServletRequest entrada = new MockHttpServletRequest();
        entrada.addHeader("Authorization", "Bearer abc");
        entrada.addHeader("X-User-Id", "autor1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(entrada));
        MockClientHttpRequest peticion = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://api.externo.com/x"));

        ejecutar(peticion);

        assertThat(peticion.getHeaders().isEmpty()).isTrue();
        assertThat(IdentityPropagationInterceptor.esInterno(URI.create("http://localhost:8081/x"))).isTrue();
        assertThat(IdentityPropagationInterceptor.esInterno(URI.create("http://catalog-service:8091/x"))).isTrue();
        assertThat(IdentityPropagationInterceptor.esInterno(URI.create("http://8.8.8.8/x"))).isFalse();
    }

    @Test
    @DisplayName("fuera de una petición (por ejemplo, una tarea programada) no agrega nada")
    void sinPeticion() throws Exception {
        MockClientHttpRequest peticion = salida();

        ejecutar(peticion);

        assertThat(peticion.getHeaders().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("la configuración automática registra el personalizador de los clientes REST")
    void autoconfiguracion() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlatformClientAutoConfiguration.class))
                .run(contexto -> assertThat(contexto).hasSingleBean(RestClientCustomizer.class));
    }
}
