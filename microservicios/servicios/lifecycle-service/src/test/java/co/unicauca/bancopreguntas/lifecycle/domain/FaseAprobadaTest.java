package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FaseAprobada")
class FaseAprobadaTest {

    final FaseDePregunta fase = new FaseAprobada();

    @Test
    void representaElEstadoAprobada() {
        assertThat(fase.estado()).isEqualTo(EstadoPregunta.APROBADA);
    }

    @Test
    void soloAvanzaAPublicada() {
        assertThat(fase.destinos()).containsExactly(EstadoPregunta.PUBLICADA);
        assertThat(fase.puedePasarA(EstadoPregunta.RECHAZADA)).isFalse();
    }

    @Test
    void noSePuedeEditar() {
        assertThat(fase.editable()).isFalse();
        assertThatThrownBy(fase::exigirEditable).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void soloAdmitePublicar() {
        OperacionesDeFase.soloAdmite(fase, "publicar", EstadoPregunta.PUBLICADA);
    }

    @Test
    void siempreSePuedeArchivar() {
        assertThat(fase.archivar()).isEqualTo(EstadoPregunta.ARCHIVADA);
    }
}
