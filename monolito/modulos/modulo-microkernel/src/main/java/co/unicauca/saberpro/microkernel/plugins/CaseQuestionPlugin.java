package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;

/** Genera preguntas de análisis de casos: el enunciado se redacta como un caso de estudio. */
public class CaseQuestionPlugin extends BaseQuestionPlugin {

    @Override
    public String getName() {
        return "case-based";
    }

    @Override
    public boolean supports(String type) {
        return "CASO".equalsIgnoreCase(type);
    }

    @Override
    protected String construirEnunciado(QuestionRequest request) {
        return "Caso de estudio: " + request.getContent();
    }
}
