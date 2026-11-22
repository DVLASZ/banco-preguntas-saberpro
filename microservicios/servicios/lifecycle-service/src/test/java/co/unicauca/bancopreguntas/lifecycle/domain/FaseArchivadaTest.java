package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FaseArchivada")
class FaseArchivadaTest {

    final FaseDePregunta fase = new FaseArchivada();

    @Test
    void representaElEstadoArchivada() {
        assertThat(fase.estado()).isEqualTo(EstadoPregunta.ARCHIVADA);
    }

    @Test
    void noTieneDestinosPropios() {
        assertThat(fase.destinos()).isEmpty();
        assertThat(fase.puedePasarA(null)).isFalse();
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
}
