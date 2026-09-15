package co.unicauca.saberpro.microkernel.pipeline.filters;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionFilter;
import co.unicauca.saberpro.preguntas.domain.Competencia;

/**
 * Valida que la clasificación (competencia) de la pregunta sea una de las
 * reconocidas por el banco de preguntas real (ej. "Lectura crítica"),
 * no cualquier texto libre.
 */
public class ClassificationFilter implements QuestionFilter {

    @Override
    public boolean process(QuestionRequest request) {
        String clasificacion = request.getClassification();
        if (clasificacion == null || clasificacion.isBlank()) {
            return false;
        }
        for (Competencia competencia : Competencia.values()) {
            if (competencia.toString().equalsIgnoreCase(clasificacion.trim())) {
                return true;
            }
        }
        return false;
    }
}
