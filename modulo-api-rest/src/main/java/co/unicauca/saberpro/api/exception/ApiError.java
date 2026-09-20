package co.unicauca.saberpro.api.exception;

import java.time.Instant;
import java.util.List;

/** Cuerpo JSON uniforme de todas las respuestas de error de la API. */
public record ApiError(Instant timestamp, int status, String error, String message, List<String> details) {

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }
}
