package co.unicauca.saberpro.revision.domain;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.revision.PreguntasDePrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** {@link FuenteDePreguntasAsignadas} solo delega en el servicio de asignación (HU-04). */
@ExtendWith(MockitoExtension.class)
class FuenteDePreguntasAsignadasTest {

    @Mock
    private AsignacionRevisionService servicio;

    @Test
    void paraRevisor_devuelveLasPreguntasQueElServicioTieneAsignadasAEseRevisor() {
        Question pregunta = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.EN_REVISION, "autor1");
        when(servicio.preguntasAsignadas("revisor1")).thenReturn(List.of(pregunta));
        FuenteDePreguntasAsignadas fuente = new FuenteDePreguntasAsignadas(servicio);

        List<Question> resultado = fuente.paraRevisor("revisor1");

        assertEquals(List.of(pregunta), resultado);
    }
}
