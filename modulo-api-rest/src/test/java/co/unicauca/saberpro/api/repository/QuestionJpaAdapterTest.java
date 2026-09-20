package co.unicauca.saberpro.api.repository;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class QuestionJpaAdapterTest {

    @Autowired
    private QuestionJpaRepository jpa;

    private QuestionJpaAdapter adapter;

    @BeforeEach
    void crearAdaptador() {
        adapter = new QuestionJpaAdapter(jpa);
    }

    private static Question pregunta(String id, String nombre, EstadoPregunta estado) {
        return new Question(id, nombre, "Enunciado de " + nombre,
                new QuestionDistractors("Opción A", "Opción B", "Opción C", "Opción D"),
                'C', estado, Competencia.COMUNICACION_ESCRITA, "Tema de prueba", Dificultad.AVANZADO);
    }

    @Test
    void crearYObtenerPorIdConservaTodosLosCampos() {
        adapter.crear(pregunta("P-001", "Primera", EstadoPregunta.BORRADOR));

        Question leida = adapter.obtenerPorId("P-001");

        assertEquals("Primera", leida.getNombre());
        assertEquals("Enunciado de Primera", leida.getEnunciado());
        assertEquals("Opción C", leida.getOpciones().getOpcionC());
        assertEquals('C', leida.getRespuestaCorrecta());
        assertEquals(EstadoPregunta.BORRADOR, leida.getEstado());
        assertEquals(Competencia.COMUNICACION_ESCRITA, leida.getCompetencia());
        assertEquals(Dificultad.AVANZADO, leida.getDificultad());
    }

    @Test
    void obtenerPorIdInexistenteRetornaNull() {
        assertNull(adapter.obtenerPorId("P-999"));
    }

    @Test
    void crearConIdRepetidoLanzaExcepcion() {
        adapter.crear(pregunta("P-001", "Primera", EstadoPregunta.BORRADOR));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.crear(pregunta("P-001", "Otra", EstadoPregunta.BORRADOR)));
    }

    @Test
    void actualizarReemplazaLosDatosDeLaPregunta() {
        adapter.crear(pregunta("P-001", "Primera", EstadoPregunta.BORRADOR));

        adapter.actualizar(pregunta("P-001", "Primera editada", EstadoPregunta.APROBADA));

        Question leida = adapter.obtenerPorId("P-001");
        assertEquals("Primera editada", leida.getNombre());
        assertEquals(EstadoPregunta.APROBADA, leida.getEstado());
    }

    @Test
    void actualizarPreguntaInexistenteLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.actualizar(pregunta("P-404", "Fantasma", EstadoPregunta.BORRADOR)));
    }

    @Test
    void obtenerTodasLasDevuelveOrdenadasPorId() {
        adapter.crear(pregunta("P-003", "Tercera", EstadoPregunta.BORRADOR));
        adapter.crear(pregunta("P-001", "Primera", EstadoPregunta.BORRADOR));
        adapter.crear(pregunta("P-002", "Segunda", EstadoPregunta.BORRADOR));

        List<String> ids = adapter.obtenerTodas().stream().map(Question::getId).toList();

        assertEquals(List.of("P-001", "P-002", "P-003"), ids);
    }

    @Test
    void generarNuevoIdEmpiezaEnP001CuandoNoHayPreguntas() {
        assertEquals("P-001", adapter.generarNuevoId());
    }

    @Test
    void generarNuevoIdContinuaDespuesDelMayorConsecutivoGuardado() {
        adapter.crear(pregunta("P-007", "Séptima", EstadoPregunta.BORRADOR));

        assertEquals("P-008", adapter.generarNuevoId());
    }

    @Test
    void generarNuevoIdNoRepiteUnIdYaEmitidoAunqueTodaviaNoSeGuarde() {
        String primero = adapter.generarNuevoId();
        String segundo = adapter.generarNuevoId();

        assertNotEquals(primero, segundo);
        assertEquals("P-001", primero);
        assertEquals("P-002", segundo);
    }

    @Test
    void generarNuevoIdIgnoraIdsQueNoSiguenElFormatoEsperado() {
        adapter.crear(pregunta("EXTRA-9", "Formato raro", EstadoPregunta.BORRADOR));
        adapter.crear(pregunta("P-abc", "Sufijo no numérico", EstadoPregunta.BORRADOR));

        assertEquals("P-001", adapter.generarNuevoId());
    }
}
