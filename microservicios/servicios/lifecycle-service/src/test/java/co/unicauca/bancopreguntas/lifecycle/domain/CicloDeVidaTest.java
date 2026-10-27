package co.unicauca.bancopreguntas.lifecycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CicloDeVida")
class CicloDeVidaTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-08T11:30:00Z");

    private static CicloDeVida borrador() {
        return CicloDeVida.registrar("Q-1", "autor-1", T0);
    }

    private static CicloDeVida rechazada() {
        return borrador().enviarARevision(T1).iniciarRevision(T1).rechazar(T1);
    }

    @Test
    @DisplayName("una pregunta nueva empieza en Borrador, versión 1, primer cambio")
    void registrar() {
        CicloDeVida c = borrador();

        assertThat(c.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(c.getVersion()).isEqualTo(1);
        assertThat(c.getSecuencia()).isEqualTo(1);
        assertThat(c.getAutorId()).isEqualTo("autor-1");
        assertThat(c.esEditable()).isTrue();
    }

    @Test
    @DisplayName("cada transición devuelve una copia con la secuencia siguiente y no toca la original")
    void devuelveUnaCopia() {
        CicloDeVida original = borrador();

        CicloDeVida enviada = original.enviarARevision(T1);

        assertThat(enviada.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        assertThat(enviada.getSecuencia()).isEqualTo(2);
        assertThat(enviada.getActualizadoEn()).isEqualTo(T1);
        assertThat(enviada.getVersion()).isEqualTo(1);
        assertThat(enviada.esEditable()).isFalse();
        assertThat(original.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(original.getSecuencia()).isEqualTo(1);
    }

    @Test
    @DisplayName("recorre el ciclo completo hasta publicar y no salta pasos")
    void cicloCompleto() {
        CicloDeVida publicada = borrador().enviarARevision(T1).iniciarRevision(T1).aprobar(T1).publicar(T1);

        assertThat(publicada.getEstado()).isEqualTo(EstadoPregunta.PUBLICADA);
        assertThat(publicada.getSecuencia()).isEqualTo(5);
        assertThatThrownBy(() -> borrador().aprobar(T1)).isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> borrador().enviarARevision(T1).publicar(T1)).isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> publicada.enviarARevision(T1)).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("una pregunta rechazada se reabre como versión nueva y vuelve a ser editable")
    void reabrir() {
        CicloDeVida reabierta = rechazada().reabrir(T1);

        assertThat(reabierta.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(reabierta.getVersion()).isEqualTo(2);
        assertThat(reabierta.getSecuencia()).isEqualTo(5);
        assertThat(reabierta.esEditable()).isTrue();
    }

    @Test
    @DisplayName("solo se reabre una pregunta rechazada")
    void reabrirSoloRechazada() {
        assertThatThrownBy(() -> borrador().reabrir(T1)).isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> borrador().enviarARevision(T1).iniciarRevision(T1).aprobar(T1).reabrir(T1))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("se puede archivar desde cualquier estado (RNF-16)")
    void archivar() {
        assertThat(borrador().archivar(T1).getEstado()).isEqualTo(EstadoPregunta.ARCHIVADA);
        assertThat(rechazada().archivar(T1).getEstado()).isEqualTo(EstadoPregunta.ARCHIVADA);
    }

    @Test
    @DisplayName("exige id, autor, estado y versión válidos")
    void invariantes() {
        assertThatThrownBy(() -> CicloDeVida.registrar(" ", "autor-1", T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CicloDeVida.registrar("Q-1", null, T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CicloDeVida("Q-1", "a", null, 1, 1, T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CicloDeVida("Q-1", "a", EstadoPregunta.BORRADOR, 0, 1, T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CicloDeVida("Q-1", "a", EstadoPregunta.BORRADOR, 1, 0, T0)).isInstanceOf(IllegalArgumentException.class);
    }
}
