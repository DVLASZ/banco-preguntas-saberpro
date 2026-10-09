package co.unicauca.saberpro.revision.access;

import co.unicauca.saberpro.revision.PreguntasDePrueba;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.revision.domain.Revisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class NotificadorCorreoSimuladoTest {

    private NotificadorCorreoSimulado notificador;
    private Revisor revisor;
    private Question pregunta;

    @BeforeEach
    void setUp() {
        notificador = new NotificadorCorreoSimulado();
        revisor = new Revisor("revisor1", "Roberto Revisor");
        pregunta = PreguntasDePrueba.pregunta("P-001", EstadoPregunta.PENDIENTE_REVISION, "autor1");
    }

    @Test
    void notificar_generaUnCorreoParaElRevisorConLosDatosDeLaPregunta() {
        notificador.notificar(revisor, pregunta);

        NotificadorCorreoSimulado.Correo correo = notificador.enviados().get(0);
        assertEquals("revisor1@unicauca.edu.co", correo.destinatario());
        assertTrue(correo.asunto().contains(pregunta.getId()));
        assertTrue(correo.cuerpo().contains(revisor.nombreCompleto()));
        assertTrue(correo.cuerpo().contains(pregunta.getId()));
        assertTrue(correo.cuerpo().contains(pregunta.getNombre()));
        assertTrue(correo.cuerpo().contains(pregunta.getCompetencia().toString()));
    }

    @Test
    void notificar_dejaRegistroDelCorreoEnviado() {
        assertTrue(notificador.enviados().isEmpty());

        notificador.notificar(revisor, pregunta);
        assertEquals(1, notificador.enviados().size());

        notificador.notificar(revisor, pregunta);
        assertEquals(2, notificador.enviados().size());
    }
}

