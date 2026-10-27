package co.unicauca.bancopreguntas.lifecycle.api.error;

import co.unicauca.bancopreguntas.lifecycle.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.lifecycle.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.lifecycle.domain.PreguntaNoRegistradaException;
import co.unicauca.bancopreguntas.platform.api.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones propias del ciclo de vida al formato {@link ApiError}. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class LifecycleExceptionHandler {

    @ExceptionHandler(PreguntaNoRegistradaException.class)
    ResponseEntity<ApiError> alNoExistirLaPregunta(PreguntaNoRegistradaException e) {
        return respuesta(HttpStatus.NOT_FOUND, "PREGUNTA_NO_ENCONTRADA", e);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<ApiError> alDenegarElAcceso(AccesoDenegadoException e) {
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", e);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    ResponseEntity<ApiError> alNoPermitirseLaOperacion(OperacionNoPermitidaException e) {
        return respuesta(HttpStatus.CONFLICT, "OPERACION_NO_PERMITIDA", e);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> alChocarDosCambios(OptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.de("CAMBIO_CONCURRENTE",
                "Otro cambio modificó la pregunta al mismo tiempo; vuelve a intentarlo."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> alRecibirUnArgumentoInvalido(IllegalArgumentException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO", e);
    }

    private static ResponseEntity<ApiError> respuesta(HttpStatus estado, String codigo, RuntimeException e) {
        return ResponseEntity.status(estado).body(ApiError.de(codigo, e.getMessage()));
    }
}
