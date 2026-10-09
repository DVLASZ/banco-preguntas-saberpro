package co.unicauca.bancopreguntas.platform.api;

import co.unicauca.bancopreguntas.platform.security.ForbiddenException;
import co.unicauca.bancopreguntas.platform.security.UnauthenticatedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Traduce los errores de la entrada HTTP y de la seguridad al formato {@link ApiError}.
 *
 * <p>Tiene la mayor precedencia, por delante de los manejadores de dominio de cada servicio: así un error de
 * binding (por ejemplo, un valor de enumeración inexistente) no lo captura un manejador genérico de
 * {@code IllegalArgumentException} a través de su causa. Los errores inesperados los resuelve
 * {@link PlatformFallbackExceptionHandler}, que va al final.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlatformExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> alValidarLaEntrada(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST,
                new ApiError("VALIDACION_ENTRADA", "Hay campos con datos inválidos", detalles));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> alViolarRestricciones(ConstraintViolationException e) {
        List<String> detalles = e.getConstraintViolations().stream().map(v -> v.getMessage()).toList();
        return respuesta(HttpStatus.BAD_REQUEST,
                new ApiError("PARAMETRO_INVALIDO", "Hay parámetros de consulta inválidos", detalles));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> alLlegarUnaPeticionMalFormada(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST,
                ApiError.de("PETICION_INVALIDA", "El cuerpo o un parámetro de la petición tiene un formato inválido"));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<ApiError> alFaltarLaIdentidad(UnauthenticatedException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, ApiError.de("NO_AUTENTICADO", e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiError> alDenegarElAcceso(ForbiddenException e) {
        return respuesta(HttpStatus.FORBIDDEN, ApiError.de("ACCESO_DENEGADO", e.getMessage()));
    }

    private static ResponseEntity<ApiError> respuesta(HttpStatus estado, ApiError error) {
        return ResponseEntity.status(estado).body(error);
    }
}
