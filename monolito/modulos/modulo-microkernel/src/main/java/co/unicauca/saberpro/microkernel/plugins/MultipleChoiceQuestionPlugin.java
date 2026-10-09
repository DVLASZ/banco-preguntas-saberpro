package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;

/** Genera preguntas de selección múltiple "normales" (HU-03 del proyecto de curso). */
public class MultipleChoiceQuestionPlugin extends BaseQuestionPlugin {

    @Override
    public String getName() {
        return "multiple-choice";
    }

    @Override
    public boolean supports(String type) {
        return "MULTIPLE_CHOICE".equalsIgnoreCase(type);
    }

    @Override
    protected String construirEnunciado(QuestionRequest request) {
        return request.getContent();
    }
}
