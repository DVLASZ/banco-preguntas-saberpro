package co.unicauca.bancopreguntas.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Notificacion")
class NotificacionTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    private static Notificacion nueva() {
        return Notificacion.nueva("n-1", "autor-1", TipoNotificacion.REVISION_APROBADA, "q-1",
                new Mensaje("Asunto", "Cuerpo"), T0);
    }

    @Test
    @DisplayName("una notificación nueva está sin leer")
    void nuevaSinLeer() {
        Notificacion n = nueva();

        assertThat(n.leida()).isFalse();
        assertThat(n.leidaEn()).isNull();
        assertThat(n.asunto()).isEqualTo("Asunto");
        assertThat(n.mensaje()).isEqualTo("Cuerpo");
    }

    @Test
    @DisplayName("marcarla como leída guarda la fecha, y hacerlo otra vez no la cambia")
    void marcarLeida() {
        Notificacion leida = nueva().marcadaComoLeida(T1);

        assertThat(leida.leida()).isTrue();
        assertThat(leida.leidaEn()).isEqualTo(T1);
        assertThat(leida.marcadaComoLeida(T1.plusSeconds(500))).isSameAs(leida);
    }

    @Test
    @DisplayName("marcarla como leída conserva el resto de los datos")
    void leidaConservaLosDatos() {
        Notificacion n = nueva().marcadaComoLeida(T1);

        assertThat(n.id()).isEqualTo("n-1");
        assertThat(n.destinatarioId()).isEqualTo("autor-1");
        assertThat(n.tipo()).isEqualTo(TipoNotificacion.REVISION_APROBADA);
        assertThat(n.asunto()).isEqualTo("Asunto");
    }

    @Test
    @DisplayName("exige identificador, destinatario, tipo, texto y fecha")
    void obligatorios() {
        Mensaje m = new Mensaje("a", "b");
        assertThatThrownBy(() -> Notificacion.nueva(" ", "u", TipoNotificacion.REVISION_APROBADA, "q", m, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notificacion.nueva("n", null, TipoNotificacion.REVISION_APROBADA, "q", m, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notificacion.nueva("n", "u", null, "q", m, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notificacion.nueva("n", "u", TipoNotificacion.REVISION_APROBADA, "q",
                new Mensaje("", "b"), T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notificacion.nueva("n", "u", TipoNotificacion.REVISION_APROBADA, "q", m, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("la página nunca tiene menos de una página y no se modifica desde fuera")
    void pagina() {
        Pagina<String> vacia = new Pagina<>(List.of(), 1, 10, 0);
        Pagina<String> trece = new Pagina<>(List.of("a"), 2, 10, 13);

        assertThat(vacia.totalPaginas()).isEqualTo(1);
        assertThat(trece.totalPaginas()).isEqualTo(2);
        assertThatThrownBy(() -> trece.elementos().add("b")).isInstanceOf(UnsupportedOperationException.class);
    }
}
