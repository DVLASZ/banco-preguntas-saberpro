package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FasePublicada")
class FasePublicadaTest {

    final FaseDePregunta fase = new FasePublicada();

    @Test
    void representaElEstadoPublicada() {
        assertThat(fase.estado()).isEqualTo(EstadoPregunta.PUBLICADA);
    }

    @Test
    void noAvanzaAOtroEstado() {
        assertThat(fase.destinos()).isEmpty();
        assertThat(fase.puedePasarA(EstadoPregunta.BORRADOR)).isFalse();
    }

    @Test
    void noSePuedeEditar() {
        assertThat(fase.editable()).isFalse();
        assertThatThrownBy(fase::exigirEditable).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void rechazaTodasLasOperaciones() {
        OperacionesDeFase.noAdmiteNinguna(fase);
    }

    @Test
    void sePuedeArchivar() {
        assertThat(fase.archivar()).isEqualTo(EstadoPregunta.ARCHIVADA);
        assertThat(fase.puedePasarA(EstadoPregunta.ARCHIVADA)).isTrue();
    }
}
