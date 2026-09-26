package co.unicauca.saberpro.api.model;

import co.unicauca.saberpro.api.dto.QuestionRequest;
import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionMapperTest {

    private static Question preguntaDeEjemplo() {
        return Question.builder().id("P-020").nombre("Patrón Observer")
                .contexto("Una aplicación debe avisar a varias ventanas cuando cambian los datos.")
                .enunciado("¿Qué hace un Subject?")
                .opciones(new QuestionDistractors("Nada", "Notifica a sus observadores", "Hereda de Observer", "Persiste datos"))
                .respuestaCorrecta('B').justificacion("El Subject notifica a quienes se suscriben.")
                .bibliografia("Gamma et al. (1994).").estado(EstadoPregunta.EN_REVISION)
                .competencia(Competencia.LECTURA_CRITICA).tema("Patrones de diseño").subtema("Observer")
                .dificultad(Dificultad.INTERMEDIO).autor("autor1").build();
    }

    @Test
    void toEntityCopiaTodosLosCamposDelDominio() {
        QuestionEntity entidad = QuestionMapper.toEntity(preguntaDeEjemplo());

        assertEquals("P-020", entidad.getId());
        assertEquals("Patrón Observer", entidad.getNombre());
        assertEquals("Una aplicación debe avisar a varias ventanas cuando cambian los datos.", entidad.getContexto());
        assertEquals("¿Qué hace un Subject?", entidad.getEnunciado());
        assertEquals("Nada", entidad.getOpcionA());
        assertEquals("Notifica a sus observadores", entidad.getOpcionB());
        assertEquals("Hereda de Observer", entidad.getOpcionC());
        assertEquals("Persiste datos", entidad.getOpcionD());
        assertEquals("B", entidad.getRespuestaCorrecta());
        assertEquals("El Subject notifica a quienes se suscriben.", entidad.getJustificacion());
        assertEquals("Gamma et al. (1994).", entidad.getBibliografia());
        assertEquals(EstadoPregunta.EN_REVISION, entidad.getEstado());
        assertEquals(Competencia.LECTURA_CRITICA, entidad.getCompetencia());
        assertEquals("Patrones de diseño", entidad.getTema());
        assertEquals("Observer", entidad.getSubtema());
        assertEquals(Dificultad.INTERMEDIO, entidad.getDificultad());
        assertEquals("autor1", entidad.getAutor());
    }

    @Test
    void ida_y_vuelta_dominio_entidad_dominio_conserva_los_datos() {
        Question original = preguntaDeEjemplo();

        Question restaurada = QuestionMapper.toDomain(QuestionMapper.toEntity(original));

        assertEquals(ContenidoPregunta.de(original), ContenidoPregunta.de(restaurada));
        assertEquals(original.getId(), restaurada.getId());
        assertEquals(original.getEstado(), restaurada.getEstado());
        assertEquals(original.getAutor(), restaurada.getAutor());
    }

    @Test
    void toResponseExponeLosCamposNuevosYLaRespuestaCorrectaComoLetra() {
        QuestionResponse respuesta = QuestionMapper.toResponse(preguntaDeEjemplo());

        assertEquals("P-020", respuesta.id());
        assertEquals("B", respuesta.respuestaCorrecta());
        assertEquals(EstadoPregunta.EN_REVISION, respuesta.estado());
        assertEquals("Persiste datos", respuesta.opcionD());
        assertEquals("Observer", respuesta.subtema());
        assertEquals("autor1", respuesta.autor());
        assertEquals("El Subject notifica a quienes se suscriben.", respuesta.justificacion());
    }

    @Test
    void toContenidoTraduceLaSolicitudAlContenidoDelDominio() {
        QuestionRequest solicitud = new QuestionRequest("Nombre", "Contexto", "¿Pregunta?", "Aa1", "Bb2", "Cc3", "Dd4",
                "c", "Justificación", "Libro", Competencia.INGLES, "Tema", "Subtema", Dificultad.AVANZADO, "autor1");

        ContenidoPregunta contenido = QuestionMapper.toContenido(solicitud);

        assertEquals("Nombre", contenido.nombre());
        assertEquals("Contexto", contenido.contexto());
        assertEquals("¿Pregunta?", contenido.enunciado());
        assertEquals("Cc3", contenido.opcionC());
        assertEquals("c", contenido.respuestaCorrecta());
        assertEquals("Subtema", contenido.subtema());
        assertEquals(Dificultad.AVANZADO, contenido.dificultad());
    }
}
