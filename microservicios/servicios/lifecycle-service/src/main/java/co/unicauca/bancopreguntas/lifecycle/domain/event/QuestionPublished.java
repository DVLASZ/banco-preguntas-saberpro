package co.unicauca.bancopreguntas.lifecycle.domain.event;

import co.unicauca.bancopreguntas.lifecycle.domain.ContenidoPublicado;

/**
 * El administrador publicó una pregunta aprobada; ya puede usarse en simulacros. El evento lleva el contenido de
 * la pregunta (transferencia de estado por evento) para que el catálogo arme su copia sin consultar al servicio de
 * preguntas: así sigue funcionando aunque ese servicio esté caído.
 */
public record QuestionPublished(String questionId, String publishedBy, int version, String title, String competency,
                                String topic, String subtopic, String difficulty, String context, String statement,
                                String optionA, String optionB, String optionC, String optionD,
                                String correctAnswer) implements DomainEvent {

    public static final String TYPE = "question.published";

    /** El evento de publicación de una pregunta, con el contenido que tiene al publicarse. */
    public static QuestionPublished de(String preguntaId, int version, String publicadaPor, ContenidoPublicado c) {
        return new QuestionPublished(preguntaId, publicadaPor, version, c.titulo(), c.competencia(), c.tema(), c.subtema(),
                c.dificultad(), c.contexto(), c.enunciado(), c.opcionA(), c.opcionB(), c.opcionC(), c.opcionD(),
                c.respuestaCorrecta());
    }

    @Override
    public String type() {
        return TYPE;
    }
}
