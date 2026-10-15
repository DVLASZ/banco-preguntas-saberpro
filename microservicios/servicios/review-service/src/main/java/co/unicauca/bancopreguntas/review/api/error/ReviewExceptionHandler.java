package co.unicauca.bancopreguntas.review.api.error;

import co.unicauca.bancopreguntas.platform.api.ApiError;
import co.unicauca.bancopreguntas.review.domain.AccesoNoPermitidoException;
import co.unicauca.bancopreguntas.review.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.review.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.review.domain.RevisionInvalidaException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones propias del dominio de revisión al formato {@link ApiError}. Los errores comunes
 * de entrada y seguridad los resuelve {@code bp-platform}.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class ReviewExceptionHandler {

    @ExceptionHandler(CasoNoEncontradoException.class)
    ResponseEntity<ApiError> alNoExistirLaRevision(CasoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, ApiError.de("REVISION_NO_ENCONTRADA", e.getMessage()));
    }

    @ExceptionHandler(AccesoNoPermitidoException.class)
    ResponseEntity<ApiError> alDenegarElAcceso(AccesoNoPermitidoException e) {
        return respuesta(HttpStatus.FORBIDDEN, ApiError.de("ACCESO_DENEGADO", e.getMessage()));
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    ResponseEntity<ApiError> alNoPermitirseLaOperacion(OperacionNoPermitidaException e) {
        return respuesta(HttpStatus.CONFLICT, ApiError.de("OPERACION_NO_PERMITIDA", e.getMessage()));
    }

    @ExceptionHandler(RevisionInvalidaException.class)
    ResponseEntity<ApiError> alSerInvalidaLaRevision(RevisionInvalidaException e) {
        return respuesta(HttpStatus.BAD_REQUEST, ApiError.de("REVISION_INVALIDA", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> alRecibirUnArgumentoInvalido(IllegalArgumentException e) {
        return respuesta(HttpStatus.BAD_REQUEST, ApiError.de("ARGUMENTO_INVALIDO", e.getMessage()));
    }

    private static ResponseEntity<ApiError> respuesta(HttpStatus estado, ApiError error) {
        return ResponseEntity.status(estado).body(error);
    }
}
