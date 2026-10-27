package co.unicauca.bancopreguntas.question.infrastructure.remote;

import co.unicauca.bancopreguntas.platform.api.ApiError;
import co.unicauca.bancopreguntas.question.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.question.domain.QuestionNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Traduce lo que responde otro servicio a las excepciones de este: un rechazo del dominio (403, 404, 409, 400) vuelve
 * a ser la misma excepción, y una caída o un error del otro servicio, un {@link DependenciaNoDisponibleException}.
 */
final class ErroresRemotos {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ErroresRemotos() {
    }

    /** Ejecuta la llamada, con el id de la pregunta para los mensajes de error. */
    static <T> T llamar(String servicio, String preguntaId, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientException e) {
            throw new DependenciaNoDisponibleException(
                    "El servicio de " + servicio + " no está disponible. Intenta de nuevo en unos minutos.", e);
        }
    }

    /** Manejador de estados de error 4xx para {@code RestClient.retrieve().onStatus(...)}. */
    static RestClient.ResponseSpec.ErrorHandler rechazos(String preguntaId) {
        return (peticion, respuesta) -> {
            ApiError error = leer(respuesta.getBody().readAllBytes());
            throw rechazo(respuesta.getStatusCode(), error.mensaje(), preguntaId);
        };
    }

    private static RuntimeException rechazo(HttpStatusCode estado, String mensaje, String preguntaId) {
        return switch (estado.value()) {
            case 403 -> new AccesoDenegadoException(mensaje);
            case 404 -> new QuestionNotFoundException(preguntaId);
            case 409 -> new OperacionNoPermitidaException(mensaje);
            default -> new IllegalArgumentException(mensaje);
        };
    }

    private static ApiError leer(byte[] cuerpo) {
        try {
            return JSON.readValue(cuerpo, ApiError.class);
        } catch (IOException e) {
            return ApiError.de("ERROR", "El otro servicio rechazó la solicitud");
        }
    }
}
