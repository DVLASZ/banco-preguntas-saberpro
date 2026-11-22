package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FaseRechazada")
class FaseRechazadaTest {

    final FaseDePregunta fase = new FaseRechazada();

    @Test
    void representaElEstadoRechazada() {
        assertThat(fase.estado()).isEqualTo(EstadoPregunta.RECHAZADA);
    }

    @Test
    void soloVuelveABorrador() {
        assertThat(fase.destinos()).containsExactly(EstadoPregunta.BORRADOR);
        assertThat(fase.puedePasarA(EstadoPregunta.PENDIENTE_REVISION)).isFalse();
    }

    @Test
    void noSePuedeEditarHastaReabrirla() {
        assertThat(fase.editable()).isFalse();
        assertThatThrownBy(fase::exigirEditable).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void soloAdmiteReabrir() {
        OperacionesDeFase.soloAdmite(fase, "reabrir", EstadoPregunta.BORRADOR);
    }

    @Test
    void siempreSePuedeArchivar() {
        assertThat(fase.archivar()).isEqualTo(EstadoPregunta.ARCHIVADA);
    }
}
