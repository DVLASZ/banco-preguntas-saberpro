package co.unicauca.bancopreguntas.history.api.error;

import co.unicauca.bancopreguntas.history.domain.HistorialNoEncontradoException;
import co.unicauca.bancopreguntas.platform.api.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones propias del dominio del historial al formato {@link ApiError}. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class HistoryExceptionHandler {

    @ExceptionHandler(HistorialNoEncontradoException.class)
    ResponseEntity<ApiError> alNoHaberHistorial(HistorialNoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.de("HISTORIAL_NO_ENCONTRADO", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> alRecibirUnArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.de("ARGUMENTO_INVALIDO", e.getMessage()));
    }
}
