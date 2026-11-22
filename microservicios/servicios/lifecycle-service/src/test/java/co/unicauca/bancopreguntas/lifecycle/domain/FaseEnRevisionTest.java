package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FaseEnRevision")
class FaseEnRevisionTest {

    final FaseDePregunta fase = new FaseEnRevision();

    @Test
    void representaElEstadoEnRevision() {
        assertThat(fase.estado()).isEqualTo(EstadoPregunta.EN_REVISION);
    }

    @Test
    void puedeTerminarAprobadaORechazada() {
        assertThat(fase.destinos()).containsExactlyInAnyOrder(EstadoPregunta.APROBADA, EstadoPregunta.RECHAZADA);
        assertThat(fase.puedePasarA(EstadoPregunta.PUBLICADA)).isFalse();
    }

    @Test
    void noSePuedeEditar() {
        assertThat(fase.editable()).isFalse();
        assertThatThrownBy(fase::exigirEditable).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void apruebaYRechaza() {
        assertThat(fase.aprobar()).isEqualTo(EstadoPregunta.APROBADA);
        assertThat(fase.rechazar()).isEqualTo(EstadoPregunta.RECHAZADA);
    }

    @Test
    void rechazaLasDemasOperaciones() {
        assertThatThrownBy(fase::enviarARevision).isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(fase::iniciarRevision).isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(fase::reabrir).isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(fase::publicar).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void siempreSePuedeArchivar() {
        assertThat(fase.archivar()).isEqualTo(EstadoPregunta.ARCHIVADA);
    }
}
