package co.unicauca.saberpro.microkernel.plugins;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;

/** Genera preguntas que referencian un recurso multimedia (imagen, audio o video). */
public class MultimediaQuestionPlugin extends BaseQuestionPlugin {

    @Override
    public String getName() {
        return "multimedia";
    }

    @Override
    public boolean supports(String type) {
        return "MULTIMEDIA".equalsIgnoreCase(type);
    }

    @Override
    protected String construirEnunciado(QuestionRequest request) {
        String recurso = request.getRecursoMultimedia();
        if (recurso == null || recurso.isBlank()) {
            recurso = "(sin recurso adjunto)";
        }
        return request.getContent() + "\n[Recurso multimedia: " + recurso + "]";
    }
}
