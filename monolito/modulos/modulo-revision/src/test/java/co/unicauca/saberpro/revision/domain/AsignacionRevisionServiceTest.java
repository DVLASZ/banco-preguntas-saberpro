package co.unicauca.saberpro.revision.domain;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import co.unicauca.saberpro.revision.PreguntasDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link AsignacionRevisionService}: una por regla de la HU-04.
 */

@ExtendWith(MockitoExtension.class)
class AsignacionRevisionServiceTest {

    private static final String ADMIN = "admin1";

    @Mock
    private QuestionService questionService;

    @Mock
    private DirectorioRevisores directorio;

    @Mock
    private AsignacionRevisionRepository repository;

    @Mock
    private NotificadorAsignacion notificador;

    private AsignacionRevisionService service;

    @BeforeEach
    void setUp() {
        service = new AsignacionRevisionService(questionService, directorio, repository, notificador);
    }

    @Test
    void preguntasPendientes_soloDevuelveLasPendientesDeRevision() {
        Question pendiente = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.PENDIENTE_REVISION, "autor1");
        Question aprobada = PreguntasDePrueba.pregunta("P-002", EstadoPregunta.APROBADA, "autor1");
        when(questionService.listarPreguntas()).thenReturn(List.of(pendiente, aprobada));

        List<Question> resultado = service.preguntasPendientes();

        assertEquals(List.of(pendiente), resultado);
    }

    @Test
    void revisoresDisponibles_noIncluyeAlAutorDeLaPregunta() {
        Question pregunta = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.PENDIENTE_REVISION, "autor1");
        Revisor autorComoRevisor = new Revisor("autor1", "Ana Autora");
        Revisor revisor1 = new Revisor("revisor1", "Roberto Revisor");
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta);
        when(directorio.revisoresActivos()).thenReturn(List.of(autorComoRevisor, revisor1));

        List<Revisor> resultado = service.revisoresDisponibles("P-001");

        assertEquals(List.of(revisor1), resultado);
    }

    @Test
    void revisoresDisponibles_soloIncluyeRevisoresActivos() {
        Question pregunta = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.PENDIENTE_REVISION, "autor1");
        Revisor revisor1 = new Revisor("revisor1", "Roberto Revisor");
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta);
        when(directorio.revisoresActivos()).thenReturn(List.of(revisor1));

        List<Revisor> resultado = service.revisoresDisponibles("P-001");

        assertEquals(List.of(revisor1), resultado);
    }

    @Test
    void asignarRevisores_conAlMenosUnRevisor_guardaLaAsignacionYPasaLaPreguntaAEnRevision() {
        Question pregunta = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.PENDIENTE_REVISION, "autor1");
        Revisor revisor1 = new Revisor("revisor1", "Roberto Revisor");
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta);
        when(directorio.revisoresActivos()).thenReturn(List.of(revisor1));

        service.asignarRevisores("P-001", List.of("revisor1"), ADMIN);

        verify(repository, times(1)).guardar(any(AsignacionRevision.class));
        verify(questionService).cambiarEstado("P-001", EstadoPregunta.EN_REVISION);
    }

    @Test
    void asignarRevisores_sinRevisores_lanzaExcepcionConElMensajeDelCriterio3() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.asignarRevisores("P-001", List.of(), ADMIN));

        assertEquals("Debe seleccionar al menos un revisor", ex.getMessage());
        verify(repository, never()).guardar(any());
        verify(questionService, never()).cambiarEstado(anyString(), any());
        verify(notificador, never()).notificar(any(), any());
    }

    @Test
    void asignarRevisores_noPermiteAsignarAlAutorComoRevisor() {
        Question pregunta = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.PENDIENTE_REVISION, "autor1");
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.asignarRevisores("P-001", List.of("autor1"), ADMIN));

        assertEquals("El autor de la pregunta no puede ser su revisor", ex.getMessage());
        verify(repository, never()).guardar(any());
        verify(questionService, never()).cambiarEstado(anyString(), any());
        verify(notificador, never()).notificar(any(), any());
    }

    @Test
    void asignarRevisores_soloFuncionaConPreguntasPendientesDeRevision() {
        Question pregunta = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.EN_REVISION, "autor1");
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.asignarRevisores("P-001", List.of("revisor1"), ADMIN));

        assertEquals("La pregunta P-001 no está pendiente de revisión", ex.getMessage());
        verify(repository, never()).guardar(any());
        verify(questionService, never()).cambiarEstado(anyString(), any());
        verify(notificador, never()).notificar(any(), any());
    }

    @Test
    void asignarRevisores_rechazaUnUsuarioQueNoEsRevisorActivo() {
        Question pregunta = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.PENDIENTE_REVISION, "autor1");
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta);
        when(directorio.revisoresActivos()).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.asignarRevisores("P-001", List.of("revisor1"), ADMIN));

        assertEquals("El usuario revisor1 no es un revisor activo", ex.getMessage());
        verify(repository, never()).guardar(any());
        verify(questionService, never()).cambiarEstado(anyString(), any());
        verify(notificador, never()).notificar(any(), any());
    }

    @Test
    void asignarRevisores_notificaACadaRevisorAsignado() {
        Question pregunta = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.PENDIENTE_REVISION, "autor1");
        Revisor revisor1 = new Revisor("revisor1", "Roberto Revisor");
        Revisor revisor2 = new Revisor("revisor2", "Rita Revisora");
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta);
        when(directorio.revisoresActivos()).thenReturn(List.of(revisor1, revisor2));

        service.asignarRevisores("P-001", List.of("revisor1", "revisor2"), ADMIN);

        verify(notificador).notificar(revisor1, pregunta);
        verify(notificador).notificar(revisor2, pregunta);
        verify(repository, times(2)).guardar(any(AsignacionRevision.class));
    }

    @Test
    void preguntasAsignadas_devuelveSoloLasDelRevisorQueSiguenEnRevision() {
        Question enRevision = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.EN_REVISION, "autor1");
        Question yaAprobada = PreguntasDePrueba.pregunta("P-002", EstadoPregunta.APROBADA, "autor1");
        AsignacionRevision a1 = new AsignacionRevision("P-001", "revisor1", ADMIN, java.time.LocalDateTime.now());
        AsignacionRevision a2 = new AsignacionRevision("P-002", "revisor1", ADMIN, java.time.LocalDateTime.now());
        when(repository.obtenerPorRevisor("revisor1")).thenReturn(List.of(a1, a2));
        when(questionService.obtenerPregunta("P-001")).thenReturn(enRevision);
        when(questionService.obtenerPregunta("P-002")).thenReturn(yaAprobada);

        List<Question> resultado = service.preguntasAsignadas("revisor1");

        assertEquals(List.of(enRevision), resultado);
    }
}

