package co.unicauca.saberpro.revision.domain;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pruebas de {@link AsignacionRevisionService}: una por regla de la HU-04.
 *
 * <p>TODO(HU-04): quitar {@code @Disabled} y escribir cada prueba (Given / When /
 * Then) con Mockito para {@code QuestionService}, {@code DirectorioRevisores},
 * {@code AsignacionRevisionRepository} y {@code NotificadorAsignacion}. Mira
 * {@code QuestionServiceTest} en modulo-preguntas como ejemplo de estilo.
 */
@Disabled("HU-04: implementar el servicio y estas pruebas")
class AsignacionRevisionServiceTest {

    @Test
    void preguntasPendientes_soloDevuelveLasPendientesDeRevision() {
        fail("HU-04: escribir esta prueba (criterio 1)");
    }

    @Test
    void revisoresDisponibles_noIncluyeAlAutorDeLaPregunta() {
        fail("HU-04: escribir esta prueba (criterio 4)");
    }

    @Test
    void revisoresDisponibles_soloIncluyeRevisoresActivos() {
        fail("HU-04: escribir esta prueba");
    }

    @Test
    void asignarRevisores_conAlMenosUnRevisor_guardaLaAsignacionYPasaLaPreguntaAEnRevision() {
        fail("HU-04: escribir esta prueba (criterio 2)");
    }

    @Test
    void asignarRevisores_sinRevisores_lanzaExcepcionConElMensajeDelCriterio3() {
        fail("HU-04: escribir esta prueba (criterio 3: \"Debe seleccionar al menos un revisor\")");
    }

    @Test
    void asignarRevisores_noPermiteAsignarAlAutorComoRevisor() {
        fail("HU-04: escribir esta prueba (criterio 4)");
    }

    @Test
    void asignarRevisores_soloFuncionaConPreguntasPendientesDeRevision() {
        fail("HU-04: escribir esta prueba");
    }

    @Test
    void asignarRevisores_notificaACadaRevisorAsignado() {
        fail("HU-04: escribir esta prueba (criterio 2: notificación simulada)");
    }

    @Test
    void preguntasAsignadas_devuelveSoloLasDelRevisorQueSiguenEnRevision() {
        fail("HU-04: escribir esta prueba");
    }
}
