package co.unicauca.bancopreguntas.audit.api.error;

import co.unicauca.bancopreguntas.platform.api.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones propias de la auditoría al formato {@link ApiError}. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class AuditExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> alRecibirUnArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.de("ARGUMENTO_INVALIDO", e.getMessage()));
    }
}
