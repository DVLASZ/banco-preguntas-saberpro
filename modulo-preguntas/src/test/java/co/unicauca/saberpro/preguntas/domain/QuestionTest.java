package co.unicauca.saberpro.preguntas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    private final QuestionDistractors opciones = new QuestionDistractors("a", "b", "c", "d");

    private Question preguntaValida(char respuesta, EstadoPregunta estado) {
        return new Question("P-001", "n", "e", opciones, respuesta, estado,
                Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO);
    }

    @Test
    void constructor_normalizaLaRespuestaCorrectaAMayuscula() {
        Question pregunta = preguntaValida('b', EstadoPregunta.BORRADOR);

        assertEquals('B', pregunta.getRespuestaCorrecta());
    }

    @Test
    void constructor_rechazaRespuestaCorrectaInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> preguntaValida('Z', EstadoPregunta.BORRADOR));
    }

    @Test
    void constructor_rechazaIdVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question(" ", "n", "e", opciones, 'A', EstadoPregunta.BORRADOR,
                        Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO));
    }

    @Test
    void constructor_rechazaCompetenciaNula() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question("P-001", "n", "e", opciones, 'A', EstadoPregunta.BORRADOR,
                        null, "tema", Dificultad.BASICO));
    }

    @Test
    void constructor_rechazaTemaVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question("P-001", "n", "e", opciones, 'A', EstadoPregunta.BORRADOR,
                        Competencia.LECTURA_CRITICA, " ", Dificultad.BASICO));
    }

    @Test
    void constructor_rechazaDificultadNula() {
        assertThrows(IllegalArgumentException.class,
                () -> new Question("P-001", "n", "e", opciones, 'A', EstadoPregunta.BORRADOR,
                        Competencia.LECTURA_CRITICA, "tema", null));
    }

    @Test
    void constructor_guardaCompetenciaTemaYDificultad() {
        Question pregunta = preguntaValida('A', EstadoPregunta.BORRADOR);

        assertEquals(Competencia.LECTURA_CRITICA, pregunta.getCompetencia());
        assertEquals("tema", pregunta.getTema());
        assertEquals(Dificultad.BASICO, pregunta.getDificultad());
    }

    @Test
    void setEstado_cambiaElEstadoActual() {
        Question pregunta = preguntaValida('A', EstadoPregunta.BORRADOR);

        pregunta.setEstado(EstadoPregunta.PUBLICADA);

        assertEquals(EstadoPregunta.PUBLICADA, pregunta.getEstado());
    }

    @Test
    void setEstado_rechazaEstadoNulo() {
        Question pregunta = preguntaValida('A', EstadoPregunta.BORRADOR);

        assertThrows(IllegalArgumentException.class, () -> pregunta.setEstado(null));
    }

    @Test
    void constructorBasico_dejaVacioElContenidoExtendidoYElAutor() {
        Question pregunta = preguntaValida('A', EstadoPregunta.BORRADOR);

        assertEquals("", pregunta.getContexto());
        assertEquals("", pregunta.getJustificacion());
        assertEquals("", pregunta.getBibliografia());
        assertEquals("", pregunta.getSubtema());
        assertEquals("", pregunta.getAutor());
    }

    @Test
    void builder_construyeLaPreguntaConTodosSusCampos() {
        Question pregunta = Question.builder().id("P-050").nombre("Nombre").contexto("Contexto")
                .enunciado("¿Pregunta?").opciones(opciones).respuestaCorrecta('c')
                .justificacion("Porque sí").bibliografia("Libro").estado(EstadoPregunta.PENDIENTE_REVISION)
                .competencia(Competencia.INGLES).tema("Tema").subtema("Subtema")
                .dificultad(Dificultad.AVANZADO).autor("autor1").build();

        assertEquals("P-050", pregunta.getId());
        assertEquals("Contexto", pregunta.getContexto());
        assertEquals("¿Pregunta?", pregunta.getEnunciado());
        assertEquals('C', pregunta.getRespuestaCorrecta());
        assertEquals("Porque sí", pregunta.getJustificacion());
        assertEquals("Libro", pregunta.getBibliografia());
        assertEquals("Subtema", pregunta.getSubtema());
        assertEquals("autor1", pregunta.getAutor());
        assertEquals(EstadoPregunta.PENDIENTE_REVISION, pregunta.getEstado());
    }

    @Test
    void builder_aplicaLasMismasInvariantesQueElConstructor() {
        Question.Builder sinEnunciado = Question.builder().id("P-050").nombre("n").opciones(opciones)
                .respuestaCorrecta('A').estado(EstadoPregunta.BORRADOR).competencia(Competencia.INGLES)
                .tema("t").dificultad(Dificultad.BASICO);

        assertThrows(IllegalArgumentException.class, sinEnunciado::build);
    }

    @Test
    void builder_tratacomoVaciosLosTextosOpcionalesNulos() {
        Question pregunta = Question.builder().id("P-050").nombre("n").enunciado("¿e?").opciones(opciones)
                .respuestaCorrecta('A').estado(EstadoPregunta.BORRADOR).competencia(Competencia.INGLES)
                .tema("t").dificultad(Dificultad.BASICO).contexto(null).autor(null).build();

        assertEquals("", pregunta.getContexto());
        assertEquals("", pregunta.getAutor());
    }

    @Test
    void toString_muestraIdYNombre() {
        assertEquals("P-001 - n", preguntaValida('A', EstadoPregunta.BORRADOR).toString());
    }
}
