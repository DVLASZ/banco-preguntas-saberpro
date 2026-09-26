package co.unicauca.saberpro.preguntas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Las cuatro opciones de respuesta (A a D) de una pregunta. */
class QuestionDistractorsTest {

    private final QuestionDistractors opciones = new QuestionDistractors("Uno", "Dos", "Tres", "Cuatro");

    @Test
    void guardaLasCuatroOpciones() {
        assertEquals("Uno", opciones.getOpcionA());
        assertEquals("Dos", opciones.getOpcionB());
        assertEquals("Tres", opciones.getOpcionC());
        assertEquals("Cuatro", opciones.getOpcionD());
    }

    @Test
    void obtenerOpcion_devuelveLaDeLaLetraSinImportarMayusculas() {
        assertEquals("Uno", opciones.obtenerOpcion('A'));
        assertEquals("Dos", opciones.obtenerOpcion('b'));
        assertEquals("Tres", opciones.obtenerOpcion('C'));
        assertEquals("Cuatro", opciones.obtenerOpcion('d'));
    }

    @Test
    void obtenerOpcion_conLetraInvalida_falla() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> opciones.obtenerOpcion('E'));

        assertTrue(error.getMessage().contains("E"));
    }

    @Test
    void exigeLasCuatroOpciones_noNulasNiVacias() {
        assertThrows(IllegalArgumentException.class, () -> new QuestionDistractors(null, "b", "c", "d"));
        assertThrows(IllegalArgumentException.class, () -> new QuestionDistractors("a", " ", "c", "d"));
        assertThrows(IllegalArgumentException.class, () -> new QuestionDistractors("a", "b", "", "d"));
        assertThrows(IllegalArgumentException.class, () -> new QuestionDistractors("a", "b", "c", null));
    }

    @Test
    void lasEtiquetasDeCompetenciaYDificultad_sonLegibles() {
        assertEquals("Lectura crítica", Competencia.LECTURA_CRITICA.toString());
        assertEquals("Inglés", Competencia.INGLES.toString());
        assertEquals("Intermedio", Dificultad.INTERMEDIO.toString());
    }
}
