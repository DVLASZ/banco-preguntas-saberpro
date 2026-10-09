package co.unicauca.saberpro.preguntas.simulacro.domain;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import co.unicauca.saberpro.preguntas.simulacro.domain.exception.SinPreguntasDisponiblesException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulacroServiceTest {

    @Mock
    private QuestionService questionService;

    @Mock
    private SimulacroRepository repository;

    private SimulacroService service;

    @BeforeEach
    void setUp() {
        service = new SimulacroService(questionService, repository);
    }

    private Question pregunta(String id, char respuestaCorrecta) {
        return new Question(id, "n-" + id, "e-" + id,
                new QuestionDistractors("a", "b", "c", "d"), respuestaCorrecta, EstadoPregunta.PUBLICADA,
                Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO);
    }

    @Test
    void generarSimulacro_tomaComoMaximoLaCantidadPedidaDePreguntasPublicadas() {
        List<Question> disponibles = List.of(pregunta("P-001", 'A'), pregunta("P-002", 'B'), pregunta("P-003", 'C'));
        when(questionService.buscarPublicadas(null, null, null)).thenReturn(disponibles);
        when(repository.generarNuevoIdSimulacro()).thenReturn("S-001");

        Simulacro simulacro = service.generarSimulacro("Grupo 8A", null, null, null, 2, 45);

        assertEquals("S-001", simulacro.getId());
        assertEquals("Grupo 8A", simulacro.getGrupo());
        assertEquals(45, simulacro.getTiempoMaximoMinutos());
        assertEquals(2, simulacro.getPreguntas().size());
        verify(repository).guardarSimulacro(simulacro);
    }

    @Test
    void generarSimulacro_usaTodasLasDisponiblesSiHayMenosQueLaCantidadPedida() {
        List<Question> disponibles = List.of(pregunta("P-001", 'A'));
        when(questionService.buscarPublicadas(null, null, null)).thenReturn(disponibles);
        when(repository.generarNuevoIdSimulacro()).thenReturn("S-001");

        Simulacro simulacro = service.generarSimulacro("Grupo 8A", null, null, null, 10, 45);

        assertEquals(1, simulacro.getPreguntas().size());
    }

    @Test
    void generarSimulacro_lanzaExcepcionSiNingunaPreguntaPublicadaCoincide() {
        when(questionService.buscarPublicadas(Competencia.INGLES, null, null)).thenReturn(List.of());

        assertThrows(SinPreguntasDisponiblesException.class,
                () -> service.generarSimulacro("Grupo 8A", Competencia.INGLES, null, null, 5, 45));
    }

    @Test
    void generarSimulacro_rechazaCantidadDePreguntasInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> service.generarSimulacro("Grupo 8A", null, null, null, 0, 45));
    }

    @Test
    void iniciarIntento_creaUnIntentoEnCursoYLoPersiste() {
        Simulacro simulacro = new Simulacro("S-001", "Grupo 8A", List.of(pregunta("P-001", 'A')), 45, null, null, null);
        when(repository.obtenerSimulacro("S-001")).thenReturn(simulacro);
        when(repository.generarNuevoIdIntento()).thenReturn("I-001");

        IntentoSimulacro intento = service.iniciarIntento("S-001", "estudiante1");

        assertEquals("I-001", intento.getId());
        assertEquals(EstadoIntento.EN_CURSO, intento.getEstado());
        verify(repository).guardarIntento(intento);
    }

    @Test
    void iniciarIntento_lanzaExcepcionSiElSimulacroNoExiste() {
        when(repository.obtenerSimulacro("S-999")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.iniciarIntento("S-999", "estudiante1"));
    }

    @Test
    void responder_persisteLaRespuestaEnElIntento() {
        IntentoSimulacro intento = new IntentoSimulacro("I-001", "S-001", "estudiante1");
        when(repository.obtenerIntento("I-001")).thenReturn(intento);

        service.responder("I-001", "P-001", 'B');

        assertEquals('B', intento.getRespuesta("P-001"));
        verify(repository).guardarIntento(intento);
    }

    @Test
    void finalizar_calificaContandoSoloLasRespuestasCorrectas() {
        Question p1 = pregunta("P-001", 'A');
        Question p2 = pregunta("P-002", 'B');
        Question p3 = pregunta("P-003", 'C');
        Simulacro simulacro = new Simulacro("S-001", "Grupo 8A", List.of(p1, p2, p3), 45, null, null, null);
        IntentoSimulacro intento = new IntentoSimulacro("I-001", "S-001", "estudiante1");
        intento.responder("P-001", 'A'); // correcta
        intento.responder("P-002", 'D'); // incorrecta
        // P-003 sin responder -> cuenta como incorrecta
        when(repository.obtenerIntento("I-001")).thenReturn(intento);
        when(repository.obtenerSimulacro("S-001")).thenReturn(simulacro);

        IntentoSimulacro resultado = service.finalizar("I-001");

        assertEquals(EstadoIntento.FINALIZADO, resultado.getEstado());
        assertEquals(1, resultado.getAciertos());
        verify(repository).guardarIntento(intento);
    }
}
