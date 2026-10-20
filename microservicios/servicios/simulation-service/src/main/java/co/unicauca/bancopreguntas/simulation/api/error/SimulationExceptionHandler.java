package co.unicauca.bancopreguntas.simulation.api.error;

import co.unicauca.bancopreguntas.platform.api.ApiError;
import co.unicauca.bancopreguntas.simulation.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.simulation.domain.CriteriosInvalidosException;
import co.unicauca.bancopreguntas.simulation.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.simulation.domain.PreguntasInsuficientesException;
import co.unicauca.bancopreguntas.simulation.domain.SimulacroNoEncontradoException;
import co.unicauca.bancopreguntas.simulation.domain.TiempoAgotadoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones propias del dominio de simulacros al formato {@link ApiError}. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class SimulationExceptionHandler {

    @ExceptionHandler(SimulacroNoEncontradoException.class)
    ResponseEntity<ApiError> alNoExistir(SimulacroNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, "SIMULACRO_NO_ENCONTRADO", e);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<ApiError> alNoSerSuyo(AccesoDenegadoException e) {
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", e);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    ResponseEntity<ApiError> alNoPoderse(OperacionNoPermitidaException e) {
        return respuesta(HttpStatus.CONFLICT, "OPERACION_NO_PERMITIDA", e);
    }

    @ExceptionHandler(TiempoAgotadoException.class)
    ResponseEntity<ApiError> alAgotarseElTiempo(TiempoAgotadoException e) {
        return respuesta(HttpStatus.CONFLICT, "TIEMPO_AGOTADO", e);
    }

    @ExceptionHandler(PreguntasInsuficientesException.class)
    ResponseEntity<ApiError> alFaltarPreguntas(PreguntasInsuficientesException e) {
        return respuesta(HttpStatus.CONFLICT, "PREGUNTAS_INSUFICIENTES", e);
    }

    @ExceptionHandler({CriteriosInvalidosException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> alSerInvalido(RuntimeException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", e);
    }

    private static ResponseEntity<ApiError> respuesta(HttpStatus estado, String codigo, RuntimeException e) {
        return ResponseEntity.status(estado).body(ApiError.de(codigo, e.getMessage()));
    }
}
