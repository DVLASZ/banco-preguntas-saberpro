package co.unicauca.bancopreguntas.question.api.error;

import co.unicauca.bancopreguntas.platform.api.ApiError;
import co.unicauca.bancopreguntas.question.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.question.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.question.domain.QuestionNotFoundException;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException;
import co.unicauca.bancopreguntas.question.domain.validation.Violacion;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Traduce las excepciones propias del dominio de preguntas al formato {@link ApiError}. Tiene la mayor
 * precedencia después de la de los errores comunes de entrada y seguridad, que resuelve el módulo
 * {@code bp-platform}, el cual también atrapa al final los errores inesperados.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class QuestionExceptionHandler {

    @ExceptionHandler(QuestionValidationException.class)
    ResponseEntity<ApiError> alIncumplirLaValidacionEstructural(QuestionValidationException e) {
        List<String> detalles = e.getViolaciones().stream()
                .map((Violacion v) -> v.campo() + ": " + v.mensaje())
                .toList();
        return respuesta(HttpStatus.BAD_REQUEST,
                new ApiError("VALIDACION_ESTRUCTURAL", "La pregunta no cumple la validación estructural", detalles));
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    ResponseEntity<ApiError> alNoExistirLaPregunta(QuestionNotFoundException e) {
        return respuesta(HttpStatus.NOT_FOUND, ApiError.de("PREGUNTA_NO_ENCONTRADA", e.getMessage()));
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<ApiError> alDenegarElAcceso(AccesoDenegadoException e) {
        return respuesta(HttpStatus.FORBIDDEN, ApiError.de("ACCESO_DENEGADO", e.getMessage()));
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    ResponseEntity<ApiError> alNoPermitirseLaOperacion(OperacionNoPermitidaException e) {
        return respuesta(HttpStatus.CONFLICT, ApiError.de("OPERACION_NO_PERMITIDA", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> alRecibirUnArgumentoInvalido(IllegalArgumentException e) {
        return respuesta(HttpStatus.BAD_REQUEST, ApiError.de("ARGUMENTO_INVALIDO", e.getMessage()));
    }

    private static ResponseEntity<ApiError> respuesta(HttpStatus estado, ApiError error) {
        return ResponseEntity.status(estado).body(error);
    }
}
