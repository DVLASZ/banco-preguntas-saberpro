package co.unicauca.saberpro.preguntas.simulacro.domain;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulacroTest {

    private Question pregunta() {
        return new Question("P-001", "n", "e", new QuestionDistractors("a", "b", "c", "d"), 'A',
                EstadoPregunta.PUBLICADA, Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO);
    }

    @Test
    void constructor_rechazaListaDePreguntasVacia() {
        assertThrows(IllegalArgumentException.class,
                () -> new Simulacro("S-001", "Grupo 8A", List.of(), 45, null, null, null));
    }

    @Test
    void constructor_rechazaTiempoMaximoNoPositivo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Simulacro("S-001", "Grupo 8A", List.of(pregunta()), 0, null, null, null));
    }

    @Test
    void constructor_rechazaGrupoVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Simulacro("S-001", " ", List.of(pregunta()), 45, null, null, null));
    }

    @Test
    void getPreguntas_esInmutable() {
        Simulacro simulacro = new Simulacro("S-001", "Grupo 8A", List.of(pregunta()), 45, null, null, null);

        assertThrows(UnsupportedOperationException.class, () -> simulacro.getPreguntas().add(pregunta()));
    }

    @Test
    void describirFiltros_indicaTodasLasCompetenciasSiNoHayFiltro() {
        Simulacro simulacro = new Simulacro("S-001", "Grupo 8A", List.of(pregunta()), 45, null, null, null);

        assertEquals("Todas las competencias", simulacro.describirFiltros());
    }

    @Test
    void describirFiltros_incluyeLosFiltrosAplicados() {
        Simulacro simulacro = new Simulacro("S-001", "Grupo 8A", List.of(pregunta()), 45,
                Competencia.INGLES, "verbos", Dificultad.AVANZADO);

        assertEquals("Inglés · verbos · Avanzado", simulacro.describirFiltros());
    }
}
