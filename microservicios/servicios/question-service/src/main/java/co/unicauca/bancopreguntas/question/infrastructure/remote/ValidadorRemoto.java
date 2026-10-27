package co.unicauca.bancopreguntas.question.infrastructure.remote;

import co.unicauca.bancopreguntas.question.application.port.ValidadorDeContenido;
import co.unicauca.bancopreguntas.question.domain.ContenidoPregunta;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException;
import co.unicauca.bancopreguntas.question.domain.validation.Violacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Adaptador del puerto {@link ValidadorDeContenido}: le pide la revisión del contenido a validation-service por REST
 * (patrón Adapter). El cliente viene de la configuración común, que le pasa la identidad de quien hizo la petición.
 */
@Component
public class ValidadorRemoto implements ValidadorDeContenido {

    private final RestClient cliente;

    public ValidadorRemoto(RestClient.Builder constructor, @Value("${question.validation.url:http://localhost:8095}") String url) {
        this.cliente = constructor.baseUrl(url).build();
    }

    @Override
    public void validarOLanzar(ContenidoPregunta contenido) {
        Resultado resultado = ErroresRemotos.llamar("validación", null, () -> cliente.post()
                .uri("/api/validation/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(contenido)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), ErroresRemotos.rechazos(null))
                .body(Resultado.class));
        if (resultado == null) {
            throw new co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException(
                    "El servicio de validación respondió sin contenido", null);
        }
        if (!resultado.valida()) {
            throw new QuestionValidationException(resultado.violaciones().stream()
                    .map(v -> new Violacion(v.campo(), v.mensaje())).toList());
        }
    }

    /** La respuesta de validation-service. */
    record Resultado(boolean valida, List<ViolacionRemota> violaciones) {

        Resultado {
            violaciones = violaciones == null ? List.of() : List.copyOf(violaciones);
        }
    }

    record ViolacionRemota(String campo, String mensaje) {
    }
}
