package co.unicauca.saberpro.microkernel.pipeline.filters;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionFilter;

/** Valida que el título y el contenido (enunciado) de la pregunta no estén vacíos. */
public class ContentValidationFilter implements QuestionFilter {

    @Override
    public boolean process(QuestionRequest request) {
        return request.getTitle() != null && !request.getTitle().isBlank()
                && request.getContent() != null && !request.getContent().isBlank();
    }
}
