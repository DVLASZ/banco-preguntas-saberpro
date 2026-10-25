package co.unicauca.bancopreguntas.platform.client;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Cuando un servicio llama a otro por REST mientras atiende una petición, le pasa la identidad de quien la hizo: el
 * token ({@code Authorization}) y las cabeceras de identidad ({@code X-User-Id} y {@code X-User-Roles}). Así el
 * servicio llamado vuelve a validar y a exigir el rol, como cualquier otro cliente (autorización redundante), y nadie
 * gana permisos por pasar por un servicio intermedio. Fuera de una petición (por ejemplo, en una tarea programada) no
 * agrega nada. Solo lo hace hacia servicios internos (nombre sin puntos, como los de la red de Docker y Eureka, o
 * localhost): el token nunca se envía a un servidor externo.
 */
public class IdentityPropagationInterceptor implements ClientHttpRequestInterceptor {

    static final List<String> CABECERAS = List.of("Authorization", "X-User-Id", "X-User-Roles");

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet && esInterno(request.getURI())) {
            for (String cabecera : CABECERAS) {
                String valor = servlet.getRequest().getHeader(cabecera);
                if (valor != null && !request.getHeaders().containsKey(cabecera)) {
                    request.getHeaders().set(cabecera, valor);
                }
            }
        }
        return execution.execute(request, body);
    }

    static boolean esInterno(URI destino) {
        String host = destino.getHost();
        if (host == null) {
            return false;
        }
        return !host.contains(".") || host.equals("127.0.0.1") || host.endsWith(".internal") || host.endsWith(".local");
    }
}
