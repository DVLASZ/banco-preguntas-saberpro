package co.unicauca.saberpro.api.exception;

import co.unicauca.saberpro.preguntas.domain.AccesoDenegadoException;
import co.unicauca.saberpro.preguntas.domain.OperacionNoPermitidaException;
import co.unicauca.saberpro.preguntas.domain.validation.QuestionValidationException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/** Convierte las excepciones del dominio y de la validacion en respuestas HTTP coherentes. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(QuestionValidationException.class)
    public ResponseEntity<ApiError> handleStructuralValidation(QuestionValidationException ex) {
        List<String> detalles = ex.getViolaciones().stream()
                .map(violacion -> violacion.campo() + ": " + violacion.mensaje())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La pregunta no cumple la validación estructural", detalles);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ApiError> handleNotAllowed(OperacionNoPermitidaException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ApiError> handleForbidden(AccesoDenegadoException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La solicitud tiene datos inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException formato && formato.getTargetType().isEnum()) {
            String campo = formato.getPath().isEmpty() ? "campo" : formato.getPath().get(0).getFieldName();
            return build(HttpStatus.BAD_REQUEST, "Valor inválido para '" + campo + "'",
                    List.of("Valores permitidos: " + nombresDe(formato.getTargetType())));
        }
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleDomainValidation(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Error inesperado procesando la solicitud", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private static String nombresDe(Class<?> tipoEnum) {
        return Arrays.stream(tipoEnum.getEnumConstants())
                .map(constante -> ((Enum<?>) constante).name())
                .toList()
                .toString();
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String mensaje, List<String> detalles) {
        ApiError cuerpo = ApiError.of(status.value(), status.getReasonPhrase(), mensaje, detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
