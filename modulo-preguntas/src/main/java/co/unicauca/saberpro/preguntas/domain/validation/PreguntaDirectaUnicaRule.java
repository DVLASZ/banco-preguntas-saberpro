package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.util.List;

/**
 * RF-09: existe una única pregunta directa. Se verifica que el enunciado
 * esté formulado como una pregunta y que contenga un solo signo de
 * interrogación de cierre.
 */
public class PreguntaDirectaUnicaRule implements ValidationRule {

    @Override
    public List<Violacion> validar(ContenidoPregunta contenido) {
        String enunciado = contenido.enunciado();
        if (Texto.vacio(enunciado)) {
            return List.of();
        }
        long signos = enunciado.chars().filter(c -> c == '?').count();
        if (signos == 0) {
            return List.of(new Violacion("enunciado",
                    "La pregunta directa debe formularse como una pregunta (con signo de interrogación)"));
        }
        if (signos > 1) {
            return List.of(new Violacion("enunciado",
                    "Debe existir una única pregunta directa: el enunciado tiene " + signos + " preguntas"));
        }
        return List.of();
    }
}
