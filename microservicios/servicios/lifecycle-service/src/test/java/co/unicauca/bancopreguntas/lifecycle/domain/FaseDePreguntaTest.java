package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FaseDePregunta (patrón State)")
class FaseDePreguntaTest {

    @ParameterizedTest
    @EnumSource(EstadoPregunta.class)
    @DisplayName("cada estado tiene su fase y la fase conoce su estado")
    void unaFasePorEstado(EstadoPregunta estado) {
        assertThat(FaseDePregunta.de(estado).estado()).isEqualTo(estado);
    }

    @ParameterizedTest
    @EnumSource(EstadoPregunta.class)
    @DisplayName("el enum y las fases coinciden en qué transición es válida")
    void coincideConElEnum(EstadoPregunta origen) {
        for (EstadoPregunta destino : EstadoPregunta.values()) {
            assertThat(FaseDePregunta.de(origen).puedePasarA(destino)).isEqualTo(origen.puedePasarA(destino));
        }
    }

    @Test
    @DisplayName("solo el borrador se puede modificar")
    void editable() {
        for (EstadoPregunta e : EstadoPregunta.values()) {
            assertThat(FaseDePregunta.de(e).editable()).isEqualTo(e == EstadoPregunta.BORRADOR);
        }
        assertThatThrownBy(() -> FaseDePregunta.de(EstadoPregunta.APROBADA).exigirEditable())
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessage("Solo se puede modificar una pregunta en estado Borrador; esta está Aprobada");
    }

    private static void soloLoPermite(Function<FaseDePregunta, EstadoPregunta> operacion, EstadoPregunta desde,
                                      EstadoPregunta hacia) {
        for (EstadoPregunta e : EstadoPregunta.values()) {
            if (e == desde) {
                assertThat(operacion.apply(FaseDePregunta.de(e))).isEqualTo(hacia);
            } else {
                assertThatThrownBy(() -> operacion.apply(FaseDePregunta.de(e)))
                        .as("desde " + e).isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("esta está " + e);
            }
        }
    }

    @Test
    @DisplayName("enviar a revisión solo desde Borrador")
    void enviar() {
        soloLoPermite(FaseDePregunta::enviarARevision, EstadoPregunta.BORRADOR, EstadoPregunta.PENDIENTE_REVISION);
    }

    @Test
    @DisplayName("iniciar la revisión solo desde Pendiente de revisión")
    void iniciar() {
        soloLoPermite(FaseDePregunta::iniciarRevision, EstadoPregunta.PENDIENTE_REVISION, EstadoPregunta.EN_REVISION);
    }

    @Test
    @DisplayName("aprobar y rechazar solo desde En revisión")
    void decidir() {
        soloLoPermite(FaseDePregunta::aprobar, EstadoPregunta.EN_REVISION, EstadoPregunta.APROBADA);
        soloLoPermite(FaseDePregunta::rechazar, EstadoPregunta.EN_REVISION, EstadoPregunta.RECHAZADA);
    }

    @Test
    @DisplayName("reabrir solo desde Rechazada y publicar solo desde Aprobada")
    void reabrirYPublicar() {
        soloLoPermite(FaseDePregunta::reabrir, EstadoPregunta.RECHAZADA, EstadoPregunta.BORRADOR);
        soloLoPermite(FaseDePregunta::publicar, EstadoPregunta.APROBADA, EstadoPregunta.PUBLICADA);
    }

    @ParameterizedTest
    @EnumSource(EstadoPregunta.class)
    @DisplayName("cualquier fase se puede archivar: nada se elimina")
    void archivar(EstadoPregunta estado) {
        assertThat(FaseDePregunta.de(estado).archivar()).isEqualTo(EstadoPregunta.ARCHIVADA);
    }

    @Test
    @DisplayName("publicada y archivada no tienen más destino que archivar")
    void sinSalida() {
        assertThat(FaseDePregunta.de(EstadoPregunta.PUBLICADA).destinos()).isEmpty();
        assertThat(FaseDePregunta.de(EstadoPregunta.ARCHIVADA).destinos()).isEmpty();
        assertThat(EnumSet.allOf(EstadoPregunta.class)).hasSize(7);
    }

    @Test
    @DisplayName("un destino nulo nunca es válido")
    void destinoNulo() {
        assertThat(FaseDePregunta.de(EstadoPregunta.BORRADOR).puedePasarA(null)).isFalse();
    }

}
