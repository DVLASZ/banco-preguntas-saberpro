package co.unicauca.bancopreguntas.question.domain.event;

import co.unicauca.bancopreguntas.question.domain.Question;

/**
 * El administrador publicó una pregunta aprobada; ya puede usarse en simulacros. El evento lleva el contenido de
 * la pregunta (transferencia de estado por evento) para que el servicio de simulacros arme su catálogo sin
 * consultar a este servicio: así sigue funcionando aunque el de preguntas esté caído.
 */
public record QuestionPublished(String questionId, String publishedBy, int version, String title, String competency,
                                String topic, String subtopic, String difficulty, String context, String statement,
                                String optionA, String optionB, String optionC, String optionD,
                                String correctAnswer) implements DomainEvent {

    public static final String TYPE = "question.published";

    /** El evento de publicación de una pregunta, con el contenido que tiene al publicarse. */
    public static QuestionPublished de(Question pregunta, String publicadaPor) {
        return new QuestionPublished(pregunta.getId(), publicadaPor, pregunta.getVersion(), pregunta.getNombre(),
                pregunta.getCompetencia().name(), pregunta.getTema(), pregunta.getSubtema(),
                pregunta.getDificultad().name(), pregunta.getContexto(), pregunta.getEnunciado(),
                pregunta.getOpciones().getOpcionA(), pregunta.getOpciones().getOpcionB(),
                pregunta.getOpciones().getOpcionC(), pregunta.getOpciones().getOpcionD(),
                String.valueOf(pregunta.getRespuestaCorrecta()));
    }

    @Override
    public String type() {
        return TYPE;
    }
}
