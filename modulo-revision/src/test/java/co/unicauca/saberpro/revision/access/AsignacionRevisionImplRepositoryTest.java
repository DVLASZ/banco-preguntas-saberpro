package co.unicauca.saberpro.revision.access;

import co.unicauca.saberpro.revision.domain.AsignacionRevision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class AsignacionRevisionImplRepositoryTest {

    private AsignacionRevisionImplRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AsignacionRevisionImplRepository();
    }

    @Test
    void guardar_y_obtenerPorPregunta_devuelveLasAsignacionesDeEsaPregunta() {
        AsignacionRevision a1 = new AsignacionRevision("P-001", "revisor1", "admin1", LocalDateTime.now());
        AsignacionRevision a2 = new AsignacionRevision("P-001", "revisor2", "admin1", LocalDateTime.now());
        AsignacionRevision otra = new AsignacionRevision("P-002", "revisor1", "admin1", LocalDateTime.now());

        repository.guardar(a1);
        repository.guardar(a2);
        repository.guardar(otra);

        assertEquals(List.of(a1, a2), repository.obtenerPorPregunta("P-001"));
    }

    @Test
    void obtenerPorRevisor_devuelveSoloLasAsignacionesDeEseRevisor() {
        AsignacionRevision deRevisor1 = new AsignacionRevision("P-001", "revisor1", "admin1", LocalDateTime.now());
        AsignacionRevision deRevisor2 = new AsignacionRevision("P-002", "revisor2", "admin1", LocalDateTime.now());

        repository.guardar(deRevisor1);
        repository.guardar(deRevisor2);

        assertEquals(List.of(deRevisor1), repository.obtenerPorRevisor("revisor1"));
    }

    @Test
    void obtenerPorPregunta_sinAsignacionesDevuelveListaVacia() {
        assertTrue(repository.obtenerPorPregunta("P-999").isEmpty());
    }
}
