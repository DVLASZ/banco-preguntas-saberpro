package co.unicauca.bancopreguntas.gateway.web;

import java.util.List;

/** Mismo formato de error que devuelven los servicios, para que el cliente reciba siempre la misma forma. */
public record ApiError(String codigo, String mensaje, List<String> detalles) {

    public static ApiError de(String codigo, String mensaje) {
        return new ApiError(codigo, mensaje, List.of());
    }
}
