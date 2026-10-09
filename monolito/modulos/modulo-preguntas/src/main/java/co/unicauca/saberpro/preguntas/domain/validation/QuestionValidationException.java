package co.unicauca.saberpro.preguntas.domain.validation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * La pregunta no cumple la validación estructural (HU03). Extiende
 * {@link IllegalArgumentException} para que quien ya atrapaba esa excepción
 * siga funcionando, y expone cada {@link Violacion} para resaltar los campos.
 */
public class QuestionValidationException extends IllegalArgumentException {

    private final List<Violacion> violaciones;

    public QuestionValidationException(List<Violacion> violaciones) {
        super("La pregunta no cumple la validación estructural: "
                + violaciones.stream().map(Violacion::mensaje).collect(Collectors.joining("; ")));
        this.violaciones = List.copyOf(violaciones);
    }

    public List<Violacion> getViolaciones() {
        return violaciones;
    }
}
