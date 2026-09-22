package co.unicauca.saberpro.revision.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pruebas de la entidad {@link AsignacionRevision}. */
class AsignacionRevisionTest {

    @Test
    void constructor_guardaTodosSusCampos() {
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 21, 10, 0);

        AsignacionRevision asignacion = new AsignacionRevision("P-001", "revisor1", "admin1", fecha);

        assertEquals("P-001", asignacion.getIdPregunta());
        assertEquals("revisor1", asignacion.getUsuarioRevisor());
        assertEquals("admin1", asignacion.getAsignadoPor());
        assertEquals(fecha, asignacion.getFecha());
    }

    @Test
    void constructor_rechazaCamposNulosOVacios() {
        LocalDateTime fecha = LocalDateTime.now();

        IllegalArgumentException sinId = assertThrows(IllegalArgumentException.class,
                () -> new AsignacionRevision(null, "revisor1", "admin1", fecha));
        assertEquals("El id de la pregunta es obligatorio", sinId.getMessage());
        IllegalArgumentException idVacio = assertThrows(IllegalArgumentException.class,
                () -> new AsignacionRevision("", "revisor1", "admin1", fecha));
        assertEquals("El id de la pregunta es obligatorio", idVacio.getMessage());

        IllegalArgumentException sinRevisor = assertThrows(IllegalArgumentException.class,
                () -> new AsignacionRevision("P-001", null, "admin1", fecha));
        assertEquals("El revisor es obligatorio", sinRevisor.getMessage());
        IllegalArgumentException revisorVacio = assertThrows(IllegalArgumentException.class,
                () -> new AsignacionRevision("P-001", "", "admin1", fecha));
        assertEquals("El revisor es obligatorio", revisorVacio.getMessage());

        IllegalArgumentException sinAsignador = assertThrows(IllegalArgumentException.class,
                () -> new AsignacionRevision("P-001", "revisor1", null, fecha));
        assertEquals("Quien asigna es obligatorio", sinAsignador.getMessage());
        IllegalArgumentException asignadorVacio = assertThrows(IllegalArgumentException.class,
                () -> new AsignacionRevision("P-001", "revisor1", "", fecha));
        assertEquals("Quien asigna es obligatorio", asignadorVacio.getMessage());

        IllegalArgumentException sinFecha = assertThrows(IllegalArgumentException.class,
                () -> new AsignacionRevision("P-001", "revisor1", "admin1", null));
        assertEquals("La fecha de la asignación es obligatoria", sinFecha.getMessage());
    }
}