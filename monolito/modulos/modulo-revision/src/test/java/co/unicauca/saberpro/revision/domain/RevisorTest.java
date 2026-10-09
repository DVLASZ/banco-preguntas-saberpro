package co.unicauca.saberpro.revision.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El revisor que puede recibir preguntas para evaluar. */
class RevisorTest {

    @Test
    void guardaSuUsuarioYSuNombreCompleto() {
        Revisor revisor = new Revisor("revisor1", "Roberto Revisor");

        assertEquals("revisor1", revisor.usuario());
        assertEquals("Roberto Revisor", revisor.nombreCompleto());
    }

    @Test
    void correo_seArmaConElUsuarioYElDominioInstitucional() {
        assertEquals("revisor1@unicauca.edu.co", new Revisor("revisor1", "Roberto Revisor").correo());
    }

    @Test
    void dosRevisoresConLosMismosDatos_sonIguales() {
        assertEquals(new Revisor("r1", "Uno"), new Revisor("r1", "Uno"));
        assertNotEquals(new Revisor("r1", "Uno"), new Revisor("r2", "Uno"));
    }
}
