package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.util.ArrayList;
import java.util.List;

/**
 * Validación estructural de una pregunta (HU03, RF-08 a RF-13): ejecuta
 * todas las reglas registradas y reúne sus violaciones, para mostrarle al
 * Autor todo lo que debe corregir de una sola vez.
 */
public class QuestionValidator {

    private final List<ValidationRule> reglas;

    public QuestionValidator(List<ValidationRule> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    /** El conjunto de reglas de HU03. */
    public static QuestionValidator porDefecto() {
        return new QuestionValidator(List.of(
                new CamposObligatoriosRule(),
                new ContextoObligatorioRule(),
                new PreguntaDirectaUnicaRule(),
                new CuatroOpcionesRule(),
                new RespuestaCorrectaUnicaRule(),
                new ExpresionesProhibidasRule(),
                new LongitudYEstructuraOpcionesRule()));
    }

    public List<Violacion> validar(ContenidoPregunta contenido) {
        List<Violacion> violaciones = new ArrayList<>();
        for (ValidationRule regla : reglas) {
            violaciones.addAll(regla.validar(contenido));
        }
        return violaciones;
    }

    /** @throws QuestionValidationException con todas las violaciones, si hay alguna */
    public void validarOLanzar(ContenidoPregunta contenido) {
        List<Violacion> violaciones = validar(contenido);
        if (!violaciones.isEmpty()) {
            throw new QuestionValidationException(violaciones);
        }
    }
}
