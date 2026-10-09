package co.unicauca.saberpro.preguntas.simulacro.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntentoSimulacroTest {

    @Test
    void constructor_empiezaEnCursoYSinRespuestas() {
        IntentoSimulacro intento = new IntentoSimulacro("I-001", "S-001", "estudiante1");

        assertEquals(EstadoIntento.EN_CURSO, intento.getEstado());
        assertNull(intento.getRespuesta("P-001"));
        assertNull(intento.getAciertos());
    }

    @Test
    void responder_normalizaLaLetraAMayuscula() {
        IntentoSimulacro intento = new IntentoSimulacro("I-001", "S-001", "estudiante1");

        intento.responder("P-001", 'b');

        assertEquals('B', intento.getRespuesta("P-001"));
    }

    @Test
    void responder_rechazaSiElIntentoYaFinalizo() {
        IntentoSimulacro intento = new IntentoSimulacro("I-001", "S-001", "estudiante1");
        intento.finalizar(0);

        assertThrows(IllegalStateException.class, () -> intento.responder("P-001", 'A'));
    }

    @Test
    void finalizar_guardaLosAciertosYCambiaElEstado() {
        IntentoSimulacro intento = new IntentoSimulacro("I-001", "S-001", "estudiante1");

        intento.finalizar(3);

        assertEquals(EstadoIntento.FINALIZADO, intento.getEstado());
        assertEquals(3, intento.getAciertos());
    }

    @Test
    void finalizar_rechazaSiYaEstabaFinalizado() {
        IntentoSimulacro intento = new IntentoSimulacro("I-001", "S-001", "estudiante1");
        intento.finalizar(3);

        assertThrows(IllegalStateException.class, () -> intento.finalizar(1));
    }
}
