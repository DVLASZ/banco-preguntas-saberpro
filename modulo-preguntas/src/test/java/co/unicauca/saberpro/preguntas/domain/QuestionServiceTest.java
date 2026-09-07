package co.unicauca.saberpro.preguntas.domain;

import co.unicauca.saberpro.preguntas.infra.Observer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository repository;

    @Mock
    private Observer observador;

    private QuestionService service;
    private Question pregunta;

    @BeforeEach
    void setUp() {
        service = new QuestionService(repository);
        pregunta = new Question("P-001", "Pregunta sobre DDD", "¿Objetivo de DDD?",
                new QuestionDistractors("a", "b", "c", "d"), 'B', EstadoPregunta.BORRADOR,
                Competencia.LECTURA_CRITICA, "DDD", Dificultad.BASICO);
    }

    @Test
    void listarPreguntas_delegaEnElRepositorio() {
        when(repository.obtenerTodas()).thenReturn(List.of(pregunta));

        List<Question> resultado = service.listarPreguntas();

        assertEquals(1, resultado.size());
        assertSame(pregunta, resultado.get(0));
    }

    @Test
    void obtenerPregunta_lanzaExcepcionSiNoExiste() {
        when(repository.obtenerPorId("NO-EXISTE")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.obtenerPregunta("NO-EXISTE"));
    }

    @Test
    void cambiarEstado_actualizaLaPreguntaYPersisteElCambio() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);

        service.cambiarEstado("P-001", EstadoPregunta.PENDIENTE_REVISION);

        assertEquals(EstadoPregunta.PENDIENTE_REVISION, pregunta.getEstado());
        verify(repository).actualizar(pregunta);
    }

    @Test
    void cambiarEstado_notificaATodosLosObservadoresSuscritos() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);
        service.agregarObservador(observador);

        service.cambiarEstado("P-001", EstadoPregunta.ARCHIVADA);

        verify(observador, times(1)).actualizar();
    }

    @Test
    void eliminarObservador_dejaDeRecibirNotificaciones() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);
        service.agregarObservador(observador);
        service.eliminarObservador(observador);

        service.cambiarEstado("P-001", EstadoPregunta.ARCHIVADA);

        verify(observador, never()).actualizar();
    }

    @Test
    void crearPregunta_quedaPendienteDeRevisionYNoAprobadaPorElAutor() {
        when(repository.generarNuevoId()).thenReturn("P-009");

        Question creada = service.crearPregunta("Nueva pregunta", "¿Enunciado?",
                new QuestionDistractors("a", "b", "c", "d"), 'A',
                Competencia.RAZONAMIENTO_CUANTITATIVO, "tema nuevo", Dificultad.INTERMEDIO);

        assertEquals("P-009", creada.getId());
        assertEquals(EstadoPregunta.PENDIENTE_REVISION, creada.getEstado());
        assertEquals(Competencia.RAZONAMIENTO_CUANTITATIVO, creada.getCompetencia());
        assertEquals("tema nuevo", creada.getTema());
        assertEquals(Dificultad.INTERMEDIO, creada.getDificultad());
        verify(repository).crear(creada);
    }

    @Test
    void crearPregunta_notificaATodosLosObservadoresSuscritos() {
        when(repository.generarNuevoId()).thenReturn("P-009");
        service.agregarObservador(observador);

        service.crearPregunta("Nueva pregunta", "¿Enunciado?",
                new QuestionDistractors("a", "b", "c", "d"), 'A',
                Competencia.RAZONAMIENTO_CUANTITATIVO, "tema nuevo", Dificultad.INTERMEDIO);

        verify(observador, times(1)).actualizar();
    }

    @Test
    void actualizarContenido_conservaElEstadoYCambiaElContenido() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);

        service.actualizarContenido("P-001", "Nuevo nombre", "¿Nuevo enunciado?",
                new QuestionDistractors("w", "x", "y", "z"), 'C',
                Competencia.INGLES, "nuevo tema", Dificultad.AVANZADO);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(repository).actualizar(captor.capture());
        Question actualizada = captor.getValue();
        assertEquals("Nuevo nombre", actualizada.getNombre());
        assertEquals(EstadoPregunta.BORRADOR, actualizada.getEstado());
        assertEquals(Competencia.INGLES, actualizada.getCompetencia());
        assertEquals("nuevo tema", actualizada.getTema());
        assertEquals(Dificultad.AVANZADO, actualizada.getDificultad());
    }

    @Test
    void contarPorEstado_agrupaCorrectamentePorEstado() {
        Question p1 = new Question("P-001", "n1", "e1",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.BORRADOR,
                Competencia.LECTURA_CRITICA, "t1", Dificultad.BASICO);
        Question p2 = new Question("P-002", "n2", "e2",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.BORRADOR,
                Competencia.LECTURA_CRITICA, "t2", Dificultad.BASICO);
        Question p3 = new Question("P-003", "n3", "e3",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.ARCHIVADA,
                Competencia.LECTURA_CRITICA, "t3", Dificultad.BASICO);
        when(repository.obtenerTodas()).thenReturn(List.of(p1, p2, p3));

        Map<EstadoPregunta, Long> conteo = service.contarPorEstado();

        assertEquals(2L, conteo.get(EstadoPregunta.BORRADOR));
        assertEquals(0L, conteo.get(EstadoPregunta.PENDIENTE_REVISION));
        assertEquals(1L, conteo.get(EstadoPregunta.ARCHIVADA));
    }

    @Test
    void buscarPublicadas_soloIncluyePreguntasPublicadas() {
        Question publicada = new Question("P-001", "n1", "e1",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.PUBLICADA,
                Competencia.LECTURA_CRITICA, "t1", Dificultad.BASICO);
        Question borrador = new Question("P-002", "n2", "e2",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.BORRADOR,
                Competencia.LECTURA_CRITICA, "t1", Dificultad.BASICO);
        when(repository.obtenerTodas()).thenReturn(List.of(publicada, borrador));

        List<Question> resultado = service.buscarPublicadas(null, null, null);

        assertEquals(List.of(publicada), resultado);
    }

    @Test
    void buscarPublicadas_filtraPorCompetenciaTemaYDificultad() {
        Question coincide = new Question("P-001", "n1", "e1",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.PUBLICADA,
                Competencia.INGLES, "Verbos irregulares", Dificultad.AVANZADO);
        Question noCoincideCompetencia = new Question("P-002", "n2", "e2",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.PUBLICADA,
                Competencia.LECTURA_CRITICA, "Verbos irregulares", Dificultad.AVANZADO);
        Question noCoincideDificultad = new Question("P-003", "n3", "e3",
                new QuestionDistractors("a", "b", "c", "d"), 'A', EstadoPregunta.PUBLICADA,
                Competencia.INGLES, "Verbos irregulares", Dificultad.BASICO);
        when(repository.obtenerTodas()).thenReturn(List.of(coincide, noCoincideCompetencia, noCoincideDificultad));

        List<Question> resultado = service.buscarPublicadas(Competencia.INGLES, "verbos", Dificultad.AVANZADO);

        assertEquals(List.of(coincide), resultado);
    }
}
