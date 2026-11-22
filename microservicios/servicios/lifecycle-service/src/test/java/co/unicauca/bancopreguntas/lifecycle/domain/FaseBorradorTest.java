package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FaseBorrador")
class FaseBorradorTest {

    final FaseDePregunta fase = new FaseBorrador();

    @Test
    void representaElEstadoBorrador() {
        assertThat(fase.estado()).isEqualTo(EstadoPregunta.BORRADOR);
    }

    @Test
    void soloAvanzaAPendienteDeRevision() {
        assertThat(fase.destinos()).containsExactly(EstadoPregunta.PENDIENTE_REVISION);
        assertThat(fase.puedePasarA(EstadoPregunta.PENDIENTE_REVISION)).isTrue();
        assertThat(fase.puedePasarA(EstadoPregunta.APROBADA)).isFalse();
    }

    @Test
    void esLaUnicaFaseEditable() {
        assertThat(fase.editable()).isTrue();
        fase.exigirEditable();
    }

    @Test
    void soloAdmiteEnviarARevision() {
        OperacionesDeFase.soloAdmite(fase, "enviarARevision", EstadoPregunta.PENDIENTE_REVISION);
    }

    @Test
    void siempreSePuedeArchivar() {
        assertThat(fase.archivar()).isEqualTo(EstadoPregunta.ARCHIVADA);
        assertThat(fase.puedePasarA(EstadoPregunta.ARCHIVADA)).isTrue();
    }
}
