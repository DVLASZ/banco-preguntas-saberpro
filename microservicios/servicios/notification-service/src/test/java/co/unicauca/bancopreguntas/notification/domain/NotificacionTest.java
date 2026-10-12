package co.unicauca.bancopreguntas.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Notificacion y plantillas")
class NotificacionTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    private static Notificacion nueva() {
        return Notificacion.nueva("n-1", "autor-1", "autor-1@correo.test", TipoNotificacion.REVISION_APROBADA, "q-1",
                new Mensaje("Asunto", "Cuerpo"), T0);
    }

    @Test
    @DisplayName("una notificación nueva está sin leer y sin correo enviado")
    void nuevaSinLeer() {
        Notificacion n = nueva();

        assertThat(n.leida()).isFalse();
        assertThat(n.leidaEn()).isNull();
        assertThat(n.correoEnviadoEn()).isNull();
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
    @DisplayName("registrar el correo enviado conserva el resto de los datos")
    void correoEnviado() {
        Notificacion n = nueva().conCorreoEnviado(T1);

        assertThat(n.correoEnviadoEn()).isEqualTo(T1);
        assertThat(n.id()).isEqualTo("n-1");
        assertThat(n.leidaEn()).isNull();
    }

    @Test
    @DisplayName("se convierte en el correo con su destinatario, asunto y cuerpo")
    void comoCorreo() {
        Correo c = nueva().comoCorreo();

        assertThat(c).isEqualTo(new Correo("autor-1@correo.test", "Asunto", "Cuerpo"));
    }

    @Test
    @DisplayName("exige identificador, destinatario, tipo, texto y fecha")
    void obligatorios() {
        Mensaje m = new Mensaje("a", "b");
        assertThatThrownBy(() -> Notificacion.nueva(" ", "u", null, TipoNotificacion.REVISION_APROBADA, "q", m, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notificacion.nueva("n", null, null, TipoNotificacion.REVISION_APROBADA, "q", m, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notificacion.nueva("n", "u", null, null, "q", m, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notificacion.nueva("n", "u", null, TipoNotificacion.REVISION_APROBADA, "q",
                new Mensaje("", "b"), T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Notificacion.nueva("n", "u", null, TipoNotificacion.REVISION_APROBADA, "q", m, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un correo exige destinatario")
    void correoSinDestinatario() {
        assertThatThrownBy(() -> new Correo(" ", "a", "b")).isInstanceOf(IllegalArgumentException.class);
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

    @Test
    @DisplayName("el aviso al revisor distingue la asignación manual de la automática")
    void plantillaDeAsignacion() {
        Mensaje manual = Plantillas.revisoresAsignados("Observer", "q-1", 1, false);
        Mensaje automatica = Plantillas.revisoresAsignados("Observer", "q-1", 2, true);

        assertThat(manual.asunto()).contains("«Observer»");
        assertThat(manual.cuerpo()).contains("administrador").contains("ronda 1");
        assertThat(automatica.cuerpo()).contains("quedas asignado otra vez").contains("ronda 2");
    }

    @Test
    @DisplayName("sin título se nombra la pregunta por su identificador")
    void sinTitulo() {
        assertThat(Plantillas.preguntaPublicada(null, "q-9").asunto()).contains("la pregunta q-9");
        assertThat(Plantillas.preguntaPublicada("  ", "q-9").asunto()).contains("la pregunta q-9");
        assertThat(Plantillas.preguntaPublicada("Observer", "q-9").cuerpo()).contains("«Observer»");
    }

    @Test
    @DisplayName("el rechazo lista las observaciones con texto e ignora las vacías")
    void plantillaDeRechazo() {
        Mensaje m = Plantillas.revisionRechazada("Observer", "q-1", 1, List.of(
                new ObservacionDeRevisor("r1", "RECHAZAR", "Falta contexto"),
                new ObservacionDeRevisor("r2", "APROBAR", null),
                new ObservacionDeRevisor("r3", "APROBAR", " ")));

        assertThat(m.asunto()).startsWith("Pregunta rechazada");
        assertThat(m.cuerpo()).contains("Observaciones:").contains("- r1 (RECHAZAR): Falta contexto")
                .doesNotContain("r2").doesNotContain("r3");
    }

    @Test
    @DisplayName("sin observaciones con texto no se agrega la sección")
    void sinObservaciones() {
        Mensaje m = Plantillas.revisionAprobada("Observer", "q-1", 1,
                List.of(new ObservacionDeRevisor("r1", "APROBAR", null)));

        assertThat(m.asunto()).startsWith("Pregunta aprobada");
        assertThat(m.cuerpo()).doesNotContain("Observaciones:").contains("publique");
    }

    @Test
    @DisplayName("el aviso de observación tardía incluye al revisor, su decisión y su texto")
    void plantillaTardia() {
        Mensaje m = Plantillas.observacionTardia("Observer", "q-1", 1, "r2", "APROBAR", "A mí me parece bien");

        assertThat(m.asunto()).startsWith("Nueva observación");
        assertThat(m.cuerpo()).contains("r2").contains("APROBAR").contains("A mí me parece bien");
        assertThat(Plantillas.observacionTardia("O", "q", 1, "r2", "APROBAR", null).cuerpo()).doesNotContain("Observación:");
    }
}
