package co.unicauca.bancopreguntas.platform.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Último recurso: atrapa lo que ningún otro manejador resolvió. Tiene la menor precedencia para no
 * interferir con los manejadores de dominio de los servicios.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class PlatformFallbackExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PlatformFallbackExceptionHandler.class);

    /**
     * Las excepciones que Spring ya expresa con un código HTTP (ruta inexistente, método no permitido, tipo de
     * contenido no soportado...) conservan ese código; el resto es un error interno y no revela detalles.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> alOcurrirOtroError(Exception e) {
        if (e instanceof ErrorResponse conEstado) {
            HttpStatusCode estado = conEstado.getStatusCode();
            HttpStatus http = HttpStatus.valueOf(estado.value());
            return ResponseEntity.status(http).body(estado.is4xxClientError()
                    ? ApiError.de("PETICION_INVALIDA", descripcion(http))
                    : ApiError.de("ERROR_INTERNO", "Ocurrió un error inesperado"));
        }
        LOG.error("Error inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.de("ERROR_INTERNO", "Ocurrió un error inesperado"));
    }

    private static String descripcion(HttpStatus estado) {
        return switch (estado) {
            case NOT_FOUND -> "El recurso pedido no existe";
            case METHOD_NOT_ALLOWED -> "El método HTTP no está permitido en esta ruta";
            case UNSUPPORTED_MEDIA_TYPE -> "El tipo de contenido no está soportado";
            case NOT_ACCEPTABLE -> "El formato de respuesta pedido no está disponible";
            default -> "La petición no es válida";
        };
    }
}
