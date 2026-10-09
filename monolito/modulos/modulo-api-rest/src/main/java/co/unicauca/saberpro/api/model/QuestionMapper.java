package co.unicauca.saberpro.api.model;

import co.unicauca.saberpro.api.dto.QuestionRequest;
import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;

/** Conversiones entre el dominio, la entidad JPA y los cuerpos JSON. */
public final class QuestionMapper {

    private QuestionMapper() {
    }

    public static QuestionEntity toEntity(Question pregunta) {
        QuestionDistractors opciones = pregunta.getOpciones();
        QuestionEntity entidad = new QuestionEntity();
        entidad.setId(pregunta.getId());
        entidad.setNombre(pregunta.getNombre());
        entidad.setContexto(pregunta.getContexto());
        entidad.setEnunciado(pregunta.getEnunciado());
        entidad.setOpcionA(opciones.getOpcionA());
        entidad.setOpcionB(opciones.getOpcionB());
        entidad.setOpcionC(opciones.getOpcionC());
        entidad.setOpcionD(opciones.getOpcionD());
        entidad.setRespuestaCorrecta(String.valueOf(pregunta.getRespuestaCorrecta()));
        entidad.setJustificacion(pregunta.getJustificacion());
        entidad.setBibliografia(pregunta.getBibliografia());
        entidad.setEstado(pregunta.getEstado());
        entidad.setCompetencia(pregunta.getCompetencia());
        entidad.setTema(pregunta.getTema());
        entidad.setSubtema(pregunta.getSubtema());
        entidad.setDificultad(pregunta.getDificultad());
        entidad.setAutor(pregunta.getAutor());
        return entidad;
    }

    public static Question toDomain(QuestionEntity entidad) {
        return Question.builder()
                .id(entidad.getId())
                .nombre(entidad.getNombre())
                .contexto(entidad.getContexto())
                .enunciado(entidad.getEnunciado())
                .opciones(new QuestionDistractors(entidad.getOpcionA(), entidad.getOpcionB(),
                        entidad.getOpcionC(), entidad.getOpcionD()))
                .respuestaCorrecta(entidad.getRespuestaCorrecta().charAt(0))
                .justificacion(entidad.getJustificacion())
                .bibliografia(entidad.getBibliografia())
                .estado(entidad.getEstado())
                .competencia(entidad.getCompetencia())
                .tema(entidad.getTema())
                .subtema(entidad.getSubtema())
                .dificultad(entidad.getDificultad())
                .autor(entidad.getAutor())
                .build();
    }

    public static QuestionResponse toResponse(Question pregunta) {
        QuestionDistractors opciones = pregunta.getOpciones();
        return new QuestionResponse(
                pregunta.getId(),
                pregunta.getNombre(),
                pregunta.getContexto(),
                pregunta.getEnunciado(),
                opciones.getOpcionA(),
                opciones.getOpcionB(),
                opciones.getOpcionC(),
                opciones.getOpcionD(),
                String.valueOf(pregunta.getRespuestaCorrecta()),
                pregunta.getJustificacion(),
                pregunta.getBibliografia(),
                pregunta.getEstado(),
                pregunta.getCompetencia(),
                pregunta.getTema(),
                pregunta.getSubtema(),
                pregunta.getDificultad(),
                pregunta.getAutor());
    }

    public static ContenidoPregunta toContenido(QuestionRequest solicitud) {
        return new ContenidoPregunta(
                solicitud.nombre(),
                solicitud.contexto(),
                solicitud.enunciado(),
                solicitud.opcionA(),
                solicitud.opcionB(),
                solicitud.opcionC(),
                solicitud.opcionD(),
                solicitud.respuestaCorrecta(),
                solicitud.justificacion(),
                solicitud.bibliografia(),
                solicitud.competencia(),
                solicitud.tema(),
                solicitud.subtema(),
                solicitud.dificultad());
    }
}
