package co.unicauca.saberpro.api.model;

import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionMapperTest {

    private static Question preguntaDeEjemplo() {
        return new Question("P-020", "Patrón Observer", "¿Qué hace un Subject?",
                new QuestionDistractors("Nada", "Notifica a sus observadores", "Hereda de Observer", "Persiste datos"),
                'B', EstadoPregunta.EN_REVISION, Competencia.LECTURA_CRITICA, "Patrones de diseño",
                Dificultad.INTERMEDIO);
    }

    @Test
    void toEntityCopiaTodosLosCamposDelDominio() {
        QuestionEntity entidad = QuestionMapper.toEntity(preguntaDeEjemplo());

        assertEquals("P-020", entidad.getId());
        assertEquals("Patrón Observer", entidad.getNombre());
        assertEquals("¿Qué hace un Subject?", entidad.getEnunciado());
        assertEquals("Nada", entidad.getOpcionA());
        assertEquals("Notifica a sus observadores", entidad.getOpcionB());
        assertEquals("Hereda de Observer", entidad.getOpcionC());
        assertEquals("Persiste datos", entidad.getOpcionD());
        assertEquals("B", entidad.getRespuestaCorrecta());
        assertEquals(EstadoPregunta.EN_REVISION, entidad.getEstado());
        assertEquals(Competencia.LECTURA_CRITICA, entidad.getCompetencia());
        assertEquals("Patrones de diseño", entidad.getTema());
        assertEquals(Dificultad.INTERMEDIO, entidad.getDificultad());
    }

    @Test
    void ida_y_vuelta_dominio_entidad_dominio_conserva_los_datos() {
        Question original = preguntaDeEjemplo();

        Question restaurada = QuestionMapper.toDomain(QuestionMapper.toEntity(original));

        assertEquals(original.getId(), restaurada.getId());
        assertEquals(original.getNombre(), restaurada.getNombre());
        assertEquals(original.getEnunciado(), restaurada.getEnunciado());
        assertEquals(original.getOpciones().getOpcionC(), restaurada.getOpciones().getOpcionC());
        assertEquals(original.getRespuestaCorrecta(), restaurada.getRespuestaCorrecta());
        assertEquals(original.getEstado(), restaurada.getEstado());
        assertEquals(original.getCompetencia(), restaurada.getCompetencia());
        assertEquals(original.getTema(), restaurada.getTema());
        assertEquals(original.getDificultad(), restaurada.getDificultad());
    }

    @Test
    void toResponseExponeLaRespuestaCorrectaComoLetra() {
        QuestionResponse respuesta = QuestionMapper.toResponse(preguntaDeEjemplo());

        assertEquals("P-020", respuesta.id());
        assertEquals("B", respuesta.respuestaCorrecta());
        assertEquals(EstadoPregunta.EN_REVISION, respuesta.estado());
        assertEquals("Persiste datos", respuesta.opcionD());
    }
}
