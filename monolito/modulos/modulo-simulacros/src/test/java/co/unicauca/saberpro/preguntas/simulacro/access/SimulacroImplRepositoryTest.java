package co.unicauca.saberpro.preguntas.simulacro.access;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import co.unicauca.saberpro.preguntas.simulacro.domain.IntentoSimulacro;
import co.unicauca.saberpro.preguntas.simulacro.domain.Simulacro;
import co.unicauca.saberpro.preguntas.simulacro.domain.SimulacroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulacroImplRepositoryTest {

    private SimulacroRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SimulacroImplRepository();
    }

    private Question pregunta() {
        return new Question("P-001", "n", "e", new QuestionDistractors("a", "b", "c", "d"), 'A',
                EstadoPregunta.PUBLICADA, Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO);
    }

    @Test
    void generarNuevoIdSimulacro_generaIdsConsecutivos() {
        assertEquals("S-001", repository.generarNuevoIdSimulacro());
        assertEquals("S-002", repository.generarNuevoIdSimulacro());
    }

    @Test
    void guardarYObtenerSimulacro_persisteElSimulacro() {
        Simulacro simulacro = new Simulacro("S-001", "Grupo 8A", List.of(pregunta()), 45, null, null, null);

        repository.guardarSimulacro(simulacro);

        assertSame(simulacro, repository.obtenerSimulacro("S-001"));
        assertEquals(List.of(simulacro), repository.listarSimulacros());
    }

    @Test
    void obtenerSimulacro_retornaNuloSiNoExiste() {
        assertNull(repository.obtenerSimulacro("NO-EXISTE"));
    }

    @Test
    void generarNuevoIdIntento_generaIdsConsecutivos() {
        assertEquals("I-001", repository.generarNuevoIdIntento());
        assertEquals("I-002", repository.generarNuevoIdIntento());
    }

    @Test
    void guardarYObtenerIntento_persisteElIntento() {
        IntentoSimulacro intento = new IntentoSimulacro("I-001", "S-001", "estudiante1");

        repository.guardarIntento(intento);

        assertSame(intento, repository.obtenerIntento("I-001"));
    }

    @Test
    void listarIntentosDeEstudiante_filtraPorUsuario() {
        IntentoSimulacro deEstudiante1 = new IntentoSimulacro("I-001", "S-001", "estudiante1");
        IntentoSimulacro deEstudiante2 = new IntentoSimulacro("I-002", "S-001", "estudiante2");
        repository.guardarIntento(deEstudiante1);
        repository.guardarIntento(deEstudiante2);

        List<IntentoSimulacro> resultado = repository.listarIntentosDeEstudiante("estudiante1");

        assertEquals(List.of(deEstudiante1), resultado);
    }
}
