package co.unicauca.bancopreguntas.user.api.error;

import co.unicauca.bancopreguntas.platform.api.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/** Traduce los errores propios de este servicio al formato {@link ApiError}. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class UserExceptionHandler {

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ApiError> alFallarKeycloak(RestClientException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiError.de("PROVEEDOR_DE_IDENTIDAD_NO_DISPONIBLE",
                "No se pudo consultar a Keycloak; se conserva el directorio actual"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> alRecibirUnArgumentoInvalido(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.de("ARGUMENTO_INVALIDO", e.getMessage()));
    }
}
