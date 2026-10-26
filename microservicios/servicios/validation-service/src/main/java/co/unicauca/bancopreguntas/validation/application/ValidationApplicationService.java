package co.unicauca.bancopreguntas.validation.application;

import co.unicauca.bancopreguntas.validation.domain.ContenidoPregunta;
import co.unicauca.bancopreguntas.validation.domain.validation.QuestionValidator;
import co.unicauca.bancopreguntas.validation.domain.validation.Violacion;

import java.util.List;

/**
 * Caso de uso único: revisar el contenido de una pregunta contra las reglas de calidad (HU-03, RF-08 a RF-13).
 * No guarda nada ni publica eventos; quien lo llama (el servicio de preguntas) decide qué hacer con el resultado.
 */
public class ValidationApplicationService {

    private final QuestionValidator validador;

    public ValidationApplicationService(QuestionValidator validador) {
        this.validador = validador;
    }

    /** Todas las violaciones del contenido; una lista vacía si cumple las reglas. */
    public List<Violacion> validar(ContenidoPregunta contenido) {
        return validador.validar(contenido);
    }
}
