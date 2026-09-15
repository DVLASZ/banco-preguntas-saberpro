package co.unicauca.saberpro.microkernel.pipeline.filters;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.pipeline.base.QuestionFilter;

/** Valida que la respuesta correcta esté presente dentro de la lista de opciones. */
public class CorrectAnswerValidationFilter implements QuestionFilter {

    @Override
    public boolean process(QuestionRequest request) {
        return request.getCorrectAnswer() != null
                && !request.getCorrectAnswer().isBlank()
                && request.getOptions() != null
                && request.getOptions().contains(request.getCorrectAnswer());
    }
}
