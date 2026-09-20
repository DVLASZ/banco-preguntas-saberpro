package co.unicauca.saberpro.api.model;

import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;

/** Conversiones entre el dominio, la entidad JPA y la respuesta JSON. */
public final class QuestionMapper {

    private QuestionMapper() {
    }

    public static QuestionEntity toEntity(Question pregunta) {
        QuestionDistractors opciones = pregunta.getOpciones();
        return new QuestionEntity(
                pregunta.getId(),
                pregunta.getNombre(),
                pregunta.getEnunciado(),
                opciones.getOpcionA(),
                opciones.getOpcionB(),
                opciones.getOpcionC(),
                opciones.getOpcionD(),
                String.valueOf(pregunta.getRespuestaCorrecta()),
                pregunta.getEstado(),
                pregunta.getCompetencia(),
                pregunta.getTema(),
                pregunta.getDificultad());
    }

    public static Question toDomain(QuestionEntity entidad) {
        return new Question(
                entidad.getId(),
                entidad.getNombre(),
                entidad.getEnunciado(),
                new QuestionDistractors(entidad.getOpcionA(), entidad.getOpcionB(),
                        entidad.getOpcionC(), entidad.getOpcionD()),
                entidad.getRespuestaCorrecta().charAt(0),
                entidad.getEstado(),
                entidad.getCompetencia(),
                entidad.getTema(),
                entidad.getDificultad());
    }

    public static QuestionResponse toResponse(Question pregunta) {
        QuestionDistractors opciones = pregunta.getOpciones();
        return new QuestionResponse(
                pregunta.getId(),
                pregunta.getNombre(),
                pregunta.getEnunciado(),
                opciones.getOpcionA(),
                opciones.getOpcionB(),
                opciones.getOpcionC(),
                opciones.getOpcionD(),
                String.valueOf(pregunta.getRespuestaCorrecta()),
                pregunta.getEstado(),
                pregunta.getCompetencia(),
                pregunta.getTema(),
                pregunta.getDificultad());
    }
}
