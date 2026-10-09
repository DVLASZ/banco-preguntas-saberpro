package co.unicauca.saberpro.preguntas.domain;

import co.unicauca.saberpro.preguntas.domain.validation.ContenidoDePrueba;
import co.unicauca.saberpro.preguntas.domain.validation.QuestionValidationException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    private static final String AUTOR = "autor1";

    @Mock
    private QuestionRepository repository;

    @Mock
    private Observer observador;

    private QuestionService service;
    private Question pregunta;

    @BeforeEach
    void setUp() {
        service = new QuestionService(repository);
        pregunta = preguntaDe("P-001", EstadoPregunta.BORRADOR, AUTOR);
    }

    /** Pregunta que cumple la validación estructural, con el estado y autor dados. */
    private static Question preguntaDe(String id, EstadoPregunta estado, String autor) {
        ContenidoPregunta c = ContenidoDePrueba.valido().build();
        return Question.builder().id(id).nombre(c.nombre()).contexto(c.contexto()).enunciado(c.enunciado())
                .opciones(new QuestionDistractors(c.opcionA(), c.opcionB(), c.opcionC(), c.opcionD()))
                .respuestaCorrecta(c.respuestaCorrecta().charAt(0)).justificacion(c.justificacion())
                .bibliografia(c.bibliografia()).estado(estado).competencia(c.competencia()).tema(c.tema())
                .subtema(c.subtema()).dificultad(c.dificultad()).autor(autor).build();
    }

    // ---- consulta ----

    @Test
    void listarPreguntas_delegaEnElRepositorio() {
        when(repository.obtenerTodas()).thenReturn(List.of(pregunta));

        List<Question> resultado = service.listarPreguntas();

        assertEquals(1, resultado.size());
        assertSame(pregunta, resultado.get(0));
    }

    @Test
    void listarPorAutor_soloDevuelveLasPreguntasDeEseAutor() {
        Question ajena = preguntaDe("P-002", EstadoPregunta.BORRADOR, "otro");
        when(repository.obtenerTodas()).thenReturn(List.of(pregunta, ajena));

        assertEquals(List.of(pregunta), service.listarPorAutor(AUTOR));
    }

    @Test
    void obtenerPregunta_lanzaExcepcionSiNoExiste() {
        when(repository.obtenerPorId("NO-EXISTE")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.obtenerPregunta("NO-EXISTE"));
    }

    // ---- HU-01: crear el borrador con validación estructural ----

    @Test
    void crearBorrador_quedaEnBorradorConSuAutorYNoEnRevision() {
        when(repository.generarNuevoId()).thenReturn("P-013");

        Question creada = service.crearBorrador(ContenidoDePrueba.valido().build(), " autor1 ");

        assertEquals("P-013", creada.getId());
        assertEquals(EstadoPregunta.BORRADOR, creada.getEstado());
        assertEquals("autor1", creada.getAutor());
        assertEquals("Observer", creada.getSubtema());
        assertFalse(creada.getContexto().isBlank());
        verify(repository).crear(creada);
    }

    @Test
    void crearBorrador_notificaATodosLosObservadoresSuscritos() {
        when(repository.generarNuevoId()).thenReturn("P-013");
        service.agregarObservador(observador);

        service.crearBorrador(ContenidoDePrueba.valido().build(), AUTOR);

        verify(observador, times(1)).actualizar();
    }

    @Test
    void crearBorrador_conContenidoInvalidoNoGuardaNadaNiNotifica() {
        service.agregarObservador(observador);

        QuestionValidationException ex = assertThrows(QuestionValidationException.class,
                () -> service.crearBorrador(ContenidoDePrueba.valido().contexto("").opcionA("Todas las anteriores").build(), AUTOR));

        assertEquals(2, ex.getViolaciones().size());
        verify(repository, never()).crear(any());
        verify(observador, never()).actualizar();
    }

    @Test
    void crearBorrador_exigeUnAutor() {
        assertThrows(IllegalArgumentException.class, () -> service.crearBorrador(ContenidoDePrueba.valido().build(), " "));
        assertThrows(IllegalArgumentException.class, () -> service.crearBorrador(ContenidoDePrueba.valido().build(), null));
    }

    @Test
    void registrarPreguntaGenerada_guardaYNotifica() {
        service.agregarObservador(observador);

        service.registrarPreguntaGenerada(pregunta);

        verify(repository).crear(pregunta);
        verify(observador).actualizar();
    }

    // ---- RF-06: modificar solo en Borrador y solo su autor ----

    @Test
    void actualizarContenido_conservaElEstadoYElAutorYCambiaElContenido() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);

        service.actualizarContenido("P-001", ContenidoDePrueba.valido().nombre("Nuevo nombre")
                .competencia(Competencia.INGLES).tema("nuevo tema").dificultad(Dificultad.AVANZADO).build(), AUTOR);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(repository).actualizar(captor.capture());
        Question actualizada = captor.getValue();
        assertEquals("Nuevo nombre", actualizada.getNombre());
        assertEquals(EstadoPregunta.BORRADOR, actualizada.getEstado());
        assertEquals(AUTOR, actualizada.getAutor());
        assertEquals(Competencia.INGLES, actualizada.getCompetencia());
        assertEquals("nuevo tema", actualizada.getTema());
        assertEquals(Dificultad.AVANZADO, actualizada.getDificultad());
    }

    @Test
    void actualizarContenido_soloPermiteModificarPreguntasEnBorrador() {
        Question enRevision = preguntaDe("P-002", EstadoPregunta.EN_REVISION, AUTOR);
        when(repository.obtenerPorId("P-002")).thenReturn(enRevision);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> service.actualizarContenido("P-002", ContenidoDePrueba.valido().build(), AUTOR));

        assertTrue(ex.getMessage().contains("Borrador"));
        verify(repository, never()).actualizar(any());
    }

    @Test
    void actualizarContenido_soloLoPuedeHacerElAutor() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);

        assertThrows(AccesoDenegadoException.class,
                () -> service.actualizarContenido("P-001", ContenidoDePrueba.valido().build(), "otro"));
        assertThrows(AccesoDenegadoException.class,
                () -> service.actualizarContenido("P-001", ContenidoDePrueba.valido().build(), null));
        verify(repository, never()).actualizar(any());
    }

    @Test
    void actualizarContenido_aplicaLaValidacionEstructural() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);

        assertThrows(QuestionValidationException.class,
                () -> service.actualizarContenido("P-001", ContenidoDePrueba.valido().justificacion("").build(), AUTOR));
        verify(repository, never()).actualizar(any());
    }

    // ---- HU-02: enviar a revisión ----

    @Test
    void enviarARevision_pasaElBorradorAPendienteDeRevision() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);
        service.agregarObservador(observador);

        service.enviarARevision("P-001", AUTOR);

        assertEquals(EstadoPregunta.PENDIENTE_REVISION, pregunta.getEstado());
        verify(repository).actualizar(pregunta);
        verify(observador, times(1)).actualizar();
    }

    @Test
    void enviarARevision_noCambiaElEstadoSiLaPreguntaNoPasaLaValidacion() {
        Question sinContexto = Question.builder().id("P-002").nombre("n").enunciado("¿Enunciado?")
                .opciones(new QuestionDistractors("Aa1", "Bb2", "Cc3", "Dd4")).respuestaCorrecta('A')
                .estado(EstadoPregunta.BORRADOR).competencia(Competencia.INGLES).tema("tema")
                .dificultad(Dificultad.BASICO).autor(AUTOR).build();
        when(repository.obtenerPorId("P-002")).thenReturn(sinContexto);

        QuestionValidationException ex = assertThrows(QuestionValidationException.class,
                () -> service.enviarARevision("P-002", AUTOR));

        assertTrue(ex.getViolaciones().stream().anyMatch(v -> v.campo().equals("contexto")));
        assertEquals(EstadoPregunta.BORRADOR, sinContexto.getEstado());
        verify(repository, never()).actualizar(any());
    }

    @Test
    void enviarARevision_soloDesdeBorrador() {
        Question yaEnviada = preguntaDe("P-002", EstadoPregunta.PENDIENTE_REVISION, AUTOR);
        when(repository.obtenerPorId("P-002")).thenReturn(yaEnviada);

        assertThrows(OperacionNoPermitidaException.class, () -> service.enviarARevision("P-002", AUTOR));
    }

    @Test
    void enviarARevision_soloLoPuedeHacerElAutor() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);

        assertThrows(AccesoDenegadoException.class, () -> service.enviarARevision("P-001", "otro"));
        assertEquals(EstadoPregunta.BORRADOR, pregunta.getEstado());
    }

    // ---- RF-15: transiciones válidas ----

    @Test
    void cambiarEstado_aplicaUnaTransicionValidaYPersisteElCambio() {
        Question pendiente = preguntaDe("P-003", EstadoPregunta.PENDIENTE_REVISION, AUTOR);
        when(repository.obtenerPorId("P-003")).thenReturn(pendiente);

        service.cambiarEstado("P-003", EstadoPregunta.EN_REVISION);

        assertEquals(EstadoPregunta.EN_REVISION, pendiente.getEstado());
        verify(repository).actualizar(pendiente);
    }

    @Test
    void cambiarEstado_rechazaUnaTransicionInvalida() {
        when(repository.obtenerPorId("P-001")).thenReturn(pregunta);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> service.cambiarEstado("P-001", EstadoPregunta.PUBLICADA));

        assertTrue(ex.getMessage().contains("Borrador") && ex.getMessage().contains("Publicada"));
        assertEquals(EstadoPregunta.BORRADOR, pregunta.getEstado());
        verify(repository, never()).actualizar(any());
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

    // ---- consultas agregadas ----

    @Test
    void contarPorEstado_agrupaCorrectamentePorEstado() {
        Question p1 = preguntaDe("P-001", EstadoPregunta.BORRADOR, AUTOR);
        Question p2 = preguntaDe("P-002", EstadoPregunta.BORRADOR, AUTOR);
        Question p3 = preguntaDe("P-003", EstadoPregunta.ARCHIVADA, AUTOR);
        when(repository.obtenerTodas()).thenReturn(List.of(p1, p2, p3));

        Map<EstadoPregunta, Long> conteo = service.contarPorEstado();

        assertEquals(2L, conteo.get(EstadoPregunta.BORRADOR));
        assertEquals(0L, conteo.get(EstadoPregunta.PENDIENTE_REVISION));
        assertEquals(1L, conteo.get(EstadoPregunta.ARCHIVADA));
    }

    @Test
    void buscarPublicadas_soloIncluyePreguntasPublicadas() {
        Question publicada = preguntaDe("P-001", EstadoPregunta.PUBLICADA, AUTOR);
        Question borrador = preguntaDe("P-002", EstadoPregunta.BORRADOR, AUTOR);
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
