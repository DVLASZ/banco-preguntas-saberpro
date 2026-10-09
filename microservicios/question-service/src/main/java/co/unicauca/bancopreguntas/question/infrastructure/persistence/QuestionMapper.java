package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import co.unicauca.bancopreguntas.question.domain.Competencia;
import co.unicauca.bancopreguntas.question.domain.Dificultad;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.Question;
import co.unicauca.bancopreguntas.question.domain.QuestionDistractors;

/** Traduce entre la entidad de dominio {@link Question} y la fila de persistencia. */
final class QuestionMapper {

    private QuestionMapper() {
    }

    static Question aDominio(QuestionEntity e) {
        return Question.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .contexto(e.getContexto())
                .enunciado(e.getEnunciado())
                .opciones(new QuestionDistractors(e.getOpcionA(), e.getOpcionB(), e.getOpcionC(), e.getOpcionD()))
                .respuestaCorrecta(e.getRespuestaCorrecta().trim().charAt(0))
                .justificacion(e.getJustificacion())
                .bibliografia(e.getBibliografia())
                .estado(EstadoPregunta.valueOf(e.getEstado()))
                .competencia(Competencia.valueOf(e.getCompetencia()))
                .tema(e.getTema())
                .subtema(e.getSubtema())
                .dificultad(Dificultad.valueOf(e.getDificultad()))
                .autor(e.getAuthorId())
                .creadaEn(e.getCreadaEn())
                .actualizadaEn(e.getActualizadaEn())
                .version(e.getQuestionVersion())
                .build();
    }

    /** Copia los datos de la pregunta sobre la entidad (nueva o ya existente). */
    static QuestionEntity aEntidad(Question p, QuestionEntity destino) {
        destino.setId(p.getId());
        destino.setAuthorId(p.getAutor());
        destino.setNombre(p.getNombre());
        destino.setContexto(p.getContexto());
        destino.setEnunciado(p.getEnunciado());
        destino.setOpcionA(p.getOpciones().getOpcionA());
        destino.setOpcionB(p.getOpciones().getOpcionB());
        destino.setOpcionC(p.getOpciones().getOpcionC());
        destino.setOpcionD(p.getOpciones().getOpcionD());
        destino.setRespuestaCorrecta(String.valueOf(p.getRespuestaCorrecta()));
        destino.setJustificacion(p.getJustificacion());
        destino.setBibliografia(p.getBibliografia());
        destino.setCompetencia(p.getCompetencia().name());
        destino.setTema(p.getTema());
        destino.setSubtema(p.getSubtema());
        destino.setDificultad(p.getDificultad().name());
        destino.setEstado(p.getEstado().name());
        destino.setCreadaEn(p.getCreadaEn());
        destino.setActualizadaEn(p.getActualizadaEn());
        destino.setQuestionVersion(p.getVersion());
        return destino;
    }
}
