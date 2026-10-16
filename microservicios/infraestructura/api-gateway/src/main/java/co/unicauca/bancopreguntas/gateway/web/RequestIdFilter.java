package co.unicauca.bancopreguntas.gateway.web;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Da a cada petición un identificador (el que traiga o uno nuevo), lo envía al servicio y lo devuelve en la
 * respuesta: con él se sigue una petición por los registros de todos los servicios. Además descarta las
 * cabeceras de identidad que un cliente intente enviar, que solo valen entre servicios.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    public static final String CABECERA = "X-Request-Id";
    private static final int LARGO_MAXIMO = 64;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String recibido = exchange.getRequest().getHeaders().getFirst(CABECERA);
        String id = recibido != null && !recibido.isBlank() && recibido.length() <= LARGO_MAXIMO
                && recibido.matches("[A-Za-z0-9._-]+") ? recibido : UUID.randomUUID().toString();
        var peticion = exchange.getRequest().mutate()
                .headers(h -> {
                    h.set(CABECERA, id);
                    h.remove("X-User-Id");
                    h.remove("X-User-Roles");
                })
                .build();
        exchange.getResponse().getHeaders().set(CABECERA, id);
        return chain.filter(exchange.mutate().request(peticion).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
