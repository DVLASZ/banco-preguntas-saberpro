package co.unicauca.bancopreguntas.assignment.api.error;

import co.unicauca.bancopreguntas.assignment.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.assignment.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.assignment.domain.RevisionInvalidaException;
import co.unicauca.bancopreguntas.platform.api.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones propias de la asignación al formato {@link ApiError}. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class AssignmentExceptionHandler {

    @ExceptionHandler(CasoNoEncontradoException.class)
    ResponseEntity<ApiError> alNoExistirLaRevision(CasoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, ApiError.de("REVISION_NO_ENCONTRADA", e.getMessage()));
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    ResponseEntity<ApiError> alNoPermitirseLaOperacion(OperacionNoPermitidaException e) {
        return respuesta(HttpStatus.CONFLICT, ApiError.de("OPERACION_NO_PERMITIDA", e.getMessage()));
    }

    @ExceptionHandler(RevisionInvalidaException.class)
    ResponseEntity<ApiError> alSerInvalidaLaAsignacion(RevisionInvalidaException e) {
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
