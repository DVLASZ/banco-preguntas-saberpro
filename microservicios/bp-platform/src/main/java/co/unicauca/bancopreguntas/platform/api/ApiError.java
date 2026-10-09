package co.unicauca.bancopreguntas.platform.api;

import java.util.List;

/** Formato único de error de todos los servicios. */
public record ApiError(String codigo, String mensaje, List<String> detalles) {

    public ApiError {
        detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }

    public static ApiError de(String codigo, String mensaje) {
        return new ApiError(codigo, mensaje, List.of());
    }
}
