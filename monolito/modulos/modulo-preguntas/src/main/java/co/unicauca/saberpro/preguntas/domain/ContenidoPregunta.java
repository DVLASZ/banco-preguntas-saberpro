package co.unicauca.saberpro.preguntas.domain;

/**
 * Datos de una pregunta tal como los diligencia el Autor (o llegan por la
 * API): todo son textos crudos, sin validar. Se separa de {@link Question}
 * para poder validar la estructura y reportar <b>todos</b> los campos
 * inválidos de una vez, en vez de fallar en el primero al construir la
 * entidad.
 */
public record ContenidoPregunta(
        String nombre,
        String contexto,
        String enunciado,
        String opcionA,
        String opcionB,
        String opcionC,
        String opcionD,
        String respuestaCorrecta,
        String justificacion,
        String bibliografia,
        Competencia competencia,
        String tema,
        String subtema,
        Dificultad dificultad) {

    /** Extrae el contenido editable de una pregunta ya existente. */
    public static ContenidoPregunta de(Question pregunta) {
        QuestionDistractors opciones = pregunta.getOpciones();
        return new ContenidoPregunta(
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
                pregunta.getCompetencia(),
                pregunta.getTema(),
                pregunta.getSubtema(),
                pregunta.getDificultad());
    }
}
