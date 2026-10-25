package co.unicauca.bancopreguntas.notification.domain.aviso;

import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.ObservacionDeRevisor;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Factory Method de avisos")
class CreadoresDeAvisoTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T10:00:00Z");

    private static Notificacion crear(TipoNotificacion tipo, ContextoDeAviso contexto) {
        return CreadoresDeAviso.para(tipo).crear("n-1", "u-1", contexto, AHORA);
    }

    @ParameterizedTest
    @EnumSource(TipoNotificacion.class)
    @DisplayName("cada tipo de notificación tiene su creador, que produce una notificación de ese tipo")
    void todosLosTiposTienenCreador(TipoNotificacion tipo) {
        Notificacion n = crear(tipo, ContextoDeAviso.de("Observer", "q-1").conEvaluacion("r1", "APROBAR", "ok"));

        assertThat(CreadoresDeAviso.para(tipo).tipo()).isEqualTo(tipo);
        assertThat(n.tipo()).isEqualTo(tipo);
        assertThat(n.preguntaId()).isEqualTo("q-1");
        assertThat(n.destinatarioId()).isEqualTo("u-1");
        assertThat(n.asunto()).isNotBlank();
        assertThat(n.mensaje()).isNotBlank();
        assertThat(n.leida()).isFalse();
    }

    @Test
    @DisplayName("cada tipo lo crea su creador concreto, que es un CreadorDeAviso")
    void creadorConcretoPorTipo() {
        assertThat(CreadoresDeAviso.para(TipoNotificacion.REVISORES_ASIGNADOS)).isExactlyInstanceOf(AvisoRevisoresAsignados.class);
        assertThat(CreadoresDeAviso.para(TipoNotificacion.PREGUNTA_EN_REVISION)).isExactlyInstanceOf(AvisoPreguntaEnRevision.class);
        assertThat(CreadoresDeAviso.para(TipoNotificacion.EVALUACION_RECIBIDA)).isExactlyInstanceOf(AvisoEvaluacionRecibida.class);
        assertThat(CreadoresDeAviso.para(TipoNotificacion.REVISION_APROBADA)).isExactlyInstanceOf(AvisoRevisionAprobada.class);
        assertThat(CreadoresDeAviso.para(TipoNotificacion.REVISION_RECHAZADA)).isExactlyInstanceOf(AvisoRevisionRechazada.class);
        assertThat(CreadoresDeAviso.para(TipoNotificacion.RESULTADO_DE_REVISION)).isExactlyInstanceOf(AvisoResultadoDeRevision.class);
        assertThat(CreadoresDeAviso.para(TipoNotificacion.OBSERVACION_TARDIA)).isExactlyInstanceOf(AvisoObservacionTardia.class);
        assertThat(CreadoresDeAviso.para(TipoNotificacion.PREGUNTA_PUBLICADA)).isExactlyInstanceOf(AvisoPreguntaPublicada.class);
        assertThat(CreadoresDeAviso.para(TipoNotificacion.PREGUNTA_PUBLICADA)).isInstanceOf(CreadorDeAviso.class);
    }

    @Test
    @DisplayName("el resultado de la revisión se redacta distinto si fue aprobada o rechazada")
    void resultadoDeRevision() {
        Notificacion aprobada = crear(TipoNotificacion.RESULTADO_DE_REVISION, ContextoDeAviso.de("Observer", "q-1").conResultado(true));
        Notificacion rechazada = crear(TipoNotificacion.RESULTADO_DE_REVISION, ContextoDeAviso.de("Observer", "q-1").conResultado(false));

        assertThat(aprobada.asunto()).startsWith("Revisión terminada, aprobada");
        assertThat(aprobada.mensaje()).contains("la publique");
        assertThat(rechazada.asunto()).startsWith("Revisión terminada, rechazada");
        assertThat(rechazada.mensaje()).contains("corregirla");
    }

    @Test
    @DisplayName("el aviso de evaluación recibida incluye al revisor, su decisión y su observación")
    void evaluacionRecibida() {
        Notificacion n = crear(TipoNotificacion.EVALUACION_RECIBIDA, ContextoDeAviso.de("Observer", "q-1").enRonda(2)
                .conEvaluacion("r1", "RECHAZAR", "Falta contexto"));

        assertThat(n.asunto()).startsWith("Evaluación recibida");
        assertThat(n.mensaje()).contains("r1").contains("RECHAZAR").contains("ronda 2").contains("Falta contexto");
    }

    @Test
    @DisplayName("el aviso al revisor distingue la asignación manual de la automática")
    void asignacion() {
        Notificacion manual = crear(TipoNotificacion.REVISORES_ASIGNADOS, ContextoDeAviso.de("Observer", "q-1").enRonda(1));
        Notificacion automatica = crear(TipoNotificacion.REVISORES_ASIGNADOS,
                ContextoDeAviso.de("Observer", "q-1").enRonda(2).asignadaAutomaticamente(true));

        assertThat(manual.asunto()).contains("«Observer»");
        assertThat(manual.mensaje()).contains("administrador").contains("ronda 1");
        assertThat(automatica.mensaje()).contains("quedas asignado otra vez").contains("ronda 2");
    }

    @Test
    @DisplayName("sin título se nombra la pregunta por su identificador")
    void sinTitulo() {
        assertThat(crear(TipoNotificacion.PREGUNTA_PUBLICADA, ContextoDeAviso.de(null, "q-9")).asunto())
                .contains("la pregunta q-9");
        assertThat(crear(TipoNotificacion.PREGUNTA_PUBLICADA, ContextoDeAviso.de("  ", "q-9")).asunto())
                .contains("la pregunta q-9");
        assertThat(crear(TipoNotificacion.PREGUNTA_PUBLICADA, ContextoDeAviso.de("Observer", "q-9")).mensaje())
                .contains("«Observer»");
    }

    @Test
    @DisplayName("el rechazo lista las observaciones con texto e ignora las vacías")
    void rechazo() {
        Notificacion n = crear(TipoNotificacion.REVISION_RECHAZADA, ContextoDeAviso.de("Observer", "q-1")
                .conObservaciones(List.of(
                        new ObservacionDeRevisor("r1", "RECHAZAR", "Falta contexto"),
                        new ObservacionDeRevisor("r2", "APROBAR", null),
                        new ObservacionDeRevisor("r3", "APROBAR", " "))));

        assertThat(n.asunto()).startsWith("Pregunta rechazada");
        assertThat(n.mensaje()).contains("Observaciones:").contains("- r1 (RECHAZAR): Falta contexto")
                .doesNotContain("r2").doesNotContain("r3");
    }

    @Test
    @DisplayName("sin observaciones con texto no se agrega la sección")
    void aprobacionSinObservaciones() {
        Notificacion n = crear(TipoNotificacion.REVISION_APROBADA, ContextoDeAviso.de("Observer", "q-1")
                .conObservaciones(List.of(new ObservacionDeRevisor("r1", "APROBAR", null))));

        assertThat(n.asunto()).startsWith("Pregunta aprobada");
        assertThat(n.mensaje()).doesNotContain("Observaciones:").contains("publique");
    }

    @Test
    @DisplayName("el aviso de observación tardía incluye al revisor, su decisión y su texto")
    void tardia() {
        Notificacion n = crear(TipoNotificacion.OBSERVACION_TARDIA, ContextoDeAviso.de("Observer", "q-1")
                .conEvaluacion("r2", "APROBAR", "A mí me parece bien"));
        Notificacion sinTexto = crear(TipoNotificacion.OBSERVACION_TARDIA, ContextoDeAviso.de("O", "q")
                .conEvaluacion("r2", "APROBAR", null));

        assertThat(n.asunto()).startsWith("Nueva observación");
        assertThat(n.mensaje()).contains("r2").contains("APROBAR").contains("A mí me parece bien");
        assertThat(sinTexto.mensaje()).doesNotContain("Observación:");
    }

    @Test
    @DisplayName("el contexto no se modifica desde fuera y tolera observaciones nulas")
    void contexto() {
        ContextoDeAviso sinObservaciones = new ContextoDeAviso("t", "q", 1, false, false, null, null, null, null);
        List<ObservacionDeRevisor> original = new java.util.ArrayList<>(List.of(new ObservacionDeRevisor("r1", "APROBAR", "x")));
        ContextoDeAviso conObservaciones = sinObservaciones.conObservaciones(original);
        original.clear();

        assertThat(sinObservaciones.observaciones()).isEmpty();
        assertThat(conObservaciones.observaciones()).hasSize(1);
    }
}
