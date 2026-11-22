package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FasePendienteDeRevision")
class FasePendienteDeRevisionTest {

    final FaseDePregunta fase = new FasePendienteDeRevision();

    @Test
    void representaElEstadoPendienteDeRevision() {
        assertThat(fase.estado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
    }

    @Test
    void soloAvanzaAEnRevision() {
        assertThat(fase.destinos()).containsExactly(EstadoPregunta.EN_REVISION);
        assertThat(fase.puedePasarA(EstadoPregunta.EN_REVISION)).isTrue();
        assertThat(fase.puedePasarA(EstadoPregunta.BORRADOR)).isFalse();
    }

    @Test
    void noSePuedeEditar() {
        assertThat(fase.editable()).isFalse();
        assertThatThrownBy(fase::exigirEditable).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void soloAdmiteIniciarLaRevision() {
        OperacionesDeFase.soloAdmite(fase, "iniciarRevision", EstadoPregunta.EN_REVISION);
    }

    @Test
    void siempreSePuedeArchivar() {
        assertThat(fase.archivar()).isEqualTo(EstadoPregunta.ARCHIVADA);
    }
}
