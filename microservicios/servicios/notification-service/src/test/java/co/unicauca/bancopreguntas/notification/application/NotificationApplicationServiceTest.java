package co.unicauca.bancopreguntas.notification.application;

import co.unicauca.bancopreguntas.notification.domain.NotificacionNoEncontradaException;
import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.ObservacionDeRevisor;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NotificationApplicationService (casos de uso)")
class NotificationApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    private InMemoryNotificationRepository notificaciones;
    private InMemoryQuestionInfoRepository preguntas;
    private RecordingEmailSender correo;
    private RelojManual reloj;
    private NotificationApplicationService servicio;

    @BeforeEach
    void preparar() {
        notificaciones = new InMemoryNotificationRepository();
        preguntas = new InMemoryQuestionInfoRepository();
        correo = new RecordingEmailSender();
        reloj = new RelojManual(T0);
        AtomicInteger ids = new AtomicInteger();
        servicio = new NotificationApplicationService(notificaciones, preguntas, correo, new InMemoryProcessedEvents(),
                reloj, () -> "n-" + ids.incrementAndGet(), usuario -> usuario + "@correo.test");
    }

    private void preguntaEnviada() {
        servicio.alEnviarseUnaPregunta("e-sub", "q-1", "autor-1", "Patrón Observer");
    }

    @Test
    @DisplayName("al enviarse una pregunta solo se recuerda su título y autor: no se avisa a nadie")
    void envioNoNotifica() {
        preguntaEnviada();

        assertThat(notificaciones.todas()).isEmpty();
        assertThat(correo.enviados()).isEmpty();
        assertThat(preguntas.buscar("q-1")).get().satisfies(i -> {
            assertThat(i.titulo()).isEqualTo("Patrón Observer");
            assertThat(i.autorId()).isEqualTo("autor-1");
        });
    }

    @Test
    @DisplayName("un reenvío actualiza el título que se recuerda")
    void reenvioActualizaTitulo() {
        preguntaEnviada();

        servicio.alEnviarseUnaPregunta("e-sub-2", "q-1", "autor-1", "Observer corregido");

        assertThat(preguntas.buscar("q-1").orElseThrow().titulo()).isEqualTo("Observer corregido");
    }

    @Test
    @DisplayName("asignar revisores avisa a cada uno, con su correo, y deja constancia del envío")
    void asignacion() {
        preguntaEnviada();

        servicio.alAsignarseRevisores("e-1", "q-1", "autor-1", 1, List.of("rev-1", "rev-2"), false, "admin-1");

        assertThat(notificaciones.todas()).filteredOn(n -> n.tipo() == TipoNotificacion.REVISORES_ASIGNADOS).hasSize(2)
                .allSatisfy(n -> {
                    assertThat(n.tipo()).isEqualTo(TipoNotificacion.REVISORES_ASIGNADOS);
                    assertThat(n.preguntaId()).isEqualTo("q-1");
                    assertThat(n.asunto()).contains("Patrón Observer");
                    assertThat(n.correoEnviadoEn()).isEqualTo(T0);
                    assertThat(n.leida()).isFalse();
                });
        assertThat(notificaciones.todas()).extracting(Notificacion::destinatarioId)
                .containsExactly("rev-1", "rev-2", "autor-1");
        assertThat(correo.enviados()).extracting(c -> c.para())
                .containsExactly("rev-1@correo.test", "rev-2@correo.test", "autor-1@correo.test");
    }

    @Test
    @DisplayName("asignar revisores avisa al autor que su pregunta entró en revisión")
    void avisoAlAutor() {
        preguntaEnviada();

        servicio.alAsignarseRevisores("e-1", "q-1", "autor-1", 1, List.of("rev-1"), false, "admin-1");

        assertThat(notificaciones.todas()).filteredOn(n -> n.destinatarioId().equals("autor-1")).singleElement()
                .satisfies(n -> {
                    assertThat(n.tipo()).isEqualTo(TipoNotificacion.PREGUNTA_EN_REVISION);
                    assertThat(n.mensaje()).contains("En revisión");
                });
    }

    @Test
    @DisplayName("el administrador que asignó recibe cada evaluación y el resultado; los revisores que evaluaron, el resultado")
    void avisosAlAdministradorYRevisores() {
        preguntaEnviada();
        servicio.alAsignarseRevisores("e-1", "q-1", "autor-1", 1, List.of("rev-1", "rev-2"), false, "admin-1");
        notificaciones.vaciar();

        servicio.alEnviarseUnaEvaluacion("e-3", "q-1", "autor-1", 1, "rev-1", "APROBAR", "Bien", false);
        assertThat(notificaciones.todas()).singleElement().satisfies(n -> {
            assertThat(n.destinatarioId()).isEqualTo("admin-1");
            assertThat(n.tipo()).isEqualTo(TipoNotificacion.EVALUACION_RECIBIDA);
            assertThat(n.mensaje()).contains("rev-1").contains("APROBAR").contains("Bien");
        });
        notificaciones.vaciar();

        servicio.alCompletarseUnaRevision("e-2", "q-1", "autor-1", 1, true, List.of(
                new ObservacionDeRevisor("rev-1", "APROBAR", "Bien"), new ObservacionDeRevisor("rev-2", "APROBAR", null)));

        assertThat(notificaciones.todas()).extracting(Notificacion::destinatarioId, Notificacion::tipo).containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("autor-1", TipoNotificacion.REVISION_APROBADA),
                org.assertj.core.groups.Tuple.tuple("admin-1", TipoNotificacion.RESULTADO_DE_REVISION),
                org.assertj.core.groups.Tuple.tuple("rev-1", TipoNotificacion.RESULTADO_DE_REVISION),
                org.assertj.core.groups.Tuple.tuple("rev-2", TipoNotificacion.RESULTADO_DE_REVISION));
    }

    @Test
    @DisplayName("una reasignación automática no cambia al administrador que ya se conocía")
    void reasignacionNoCambiaAdministrador() {
        preguntaEnviada();
        servicio.alAsignarseRevisores("e-1", "q-1", "autor-1", 1, List.of("rev-1"), false, "admin-1");

        servicio.alAsignarseRevisores("e-2", "q-1", "autor-1", 2, List.of("rev-1"), true, "system");
        servicio.alEnviarseUnaPregunta("e-sub-2", "q-1", "autor-1", "Otro título");

        assertThat(preguntas.buscar("q-1").orElseThrow().administradorId()).isEqualTo("admin-1");
    }

    @Test
    @DisplayName("la reasignación automática se redacta distinto")
    void reasignacionAutomatica() {
        preguntaEnviada();

        servicio.alAsignarseRevisores("e-1", "q-1", null, 2, List.of("rev-1"), true, null);

        assertThat(notificaciones.todas().get(0).mensaje()).contains("quedas asignado otra vez");
    }

    @Test
    @DisplayName("sin conocer la pregunta igual se avisa, nombrándola por su identificador")
    void preguntaDesconocida() {
        servicio.alAsignarseRevisores("e-1", "q-9", null, 1, List.of("rev-1"), false, null);

        assertThat(notificaciones.todas().get(0).asunto()).contains("la pregunta q-9");
    }

    @Test
    @DisplayName("una revisión rechazada avisa al autor con las observaciones")
    void rechazada() {
        preguntaEnviada();

        servicio.alCompletarseUnaRevision("e-2", "q-1", "autor-1", 1, false,
                List.of(new ObservacionDeRevisor("rev-1", "RECHAZAR", "Falta contexto")));

        Notificacion n = notificaciones.todas().get(0);
        assertThat(n.destinatarioId()).isEqualTo("autor-1");
        assertThat(n.tipo()).isEqualTo(TipoNotificacion.REVISION_RECHAZADA);
        assertThat(n.mensaje()).contains("Falta contexto");
    }

    @Test
    @DisplayName("una revisión aprobada avisa al autor")
    void aprobada() {
        preguntaEnviada();

        servicio.alCompletarseUnaRevision("e-2", "q-1", "autor-1", 1, true, List.of());

        assertThat(notificaciones.todas()).singleElement()
                .satisfies(n -> assertThat(n.tipo()).isEqualTo(TipoNotificacion.REVISION_APROBADA));
    }

    @Test
    @DisplayName("solo las evaluaciones tardías avisan al autor")
    void evaluaciones() {
        preguntaEnviada();

        servicio.alEnviarseUnaEvaluacion("e-3", "q-1", "autor-1", 1, "rev-1", "APROBAR", null, false);
        assertThat(notificaciones.todas()).isEmpty();

        servicio.alEnviarseUnaEvaluacion("e-4", "q-1", "autor-1", 1, "rev-2", "APROBAR", "Yo la veo bien", true);
        assertThat(notificaciones.todas()).singleElement().satisfies(n -> {
            assertThat(n.tipo()).isEqualTo(TipoNotificacion.OBSERVACION_TARDIA);
            assertThat(n.destinatarioId()).isEqualTo("autor-1");
            assertThat(n.mensaje()).contains("Yo la veo bien");
        });
    }

    @Test
    @DisplayName("al publicarse la pregunta se avisa a su autor, conocido por el envío")
    void publicada() {
        preguntaEnviada();

        servicio.alPublicarseUnaPregunta("e-5", "q-1");

        assertThat(notificaciones.todas()).singleElement().satisfies(n -> {
            assertThat(n.tipo()).isEqualTo(TipoNotificacion.PREGUNTA_PUBLICADA);
            assertThat(n.destinatarioId()).isEqualTo("autor-1");
        });
    }

    @Test
    @DisplayName("si no se conoce al autor, la publicación no avisa a nadie ni falla")
    void publicadaSinAutor() {
        servicio.alPublicarseUnaPregunta("e-5", "q-desconocida");

        assertThat(notificaciones.todas()).isEmpty();
    }

    @Test
    @DisplayName("un evento repetido no duplica los avisos")
    void idempotente() {
        preguntaEnviada();

        servicio.alAsignarseRevisores("e-1", "q-1", null, 1, List.of("rev-1"), false, null);
        servicio.alAsignarseRevisores("e-1", "q-1", null, 1, List.of("rev-1"), false, null);
        servicio.alCompletarseUnaRevision("e-2", "q-1", "autor-1", 1, true, List.of());
        servicio.alCompletarseUnaRevision("e-2", "q-1", "autor-1", 1, true, List.of());

        assertThat(notificaciones.todas()).hasSize(2);
        assertThat(correo.enviados()).hasSize(2);
    }

    @Test
    @DisplayName("si el correo falla, la notificación igual queda en la bandeja, sin marca de envío")
    void correoCaido() {
        preguntaEnviada();
        correo.fallar = true;

        servicio.alCompletarseUnaRevision("e-2", "q-1", "autor-1", 1, true, List.of());

        assertThat(notificaciones.todas()).singleElement().satisfies(n -> assertThat(n.correoEnviadoEn()).isNull());
        assertThat(servicio.noLeidas("autor-1")).isEqualTo(1);
        assertThat(servicio.correosEnviados(1, 10).elementos()).isEmpty();
    }

    // ---------------------------------------------------------------- bandeja

    private void variasNotificaciones() {
        preguntaEnviada();
        servicio.alCompletarseUnaRevision("e-a", "q-1", "autor-1", 1, false, List.of());
        reloj.avanzar(Duration.ofMinutes(1));
        servicio.alCompletarseUnaRevision("e-b", "q-1", "autor-1", 2, true, List.of());
        reloj.avanzar(Duration.ofMinutes(1));
        servicio.alPublicarseUnaPregunta("e-c", "q-1");
        servicio.alAsignarseRevisores("e-d", "q-1", null, 1, List.of("rev-1"), false, null);
    }

    @Test
    @DisplayName("la bandeja es solo del usuario y trae la más reciente primero")
    void bandeja() {
        variasNotificaciones();

        var autor = servicio.bandeja("autor-1", false, 1, 10);

        assertThat(autor.totalElementos()).isEqualTo(3);
        assertThat(autor.elementos()).extracting(Notificacion::tipo).containsExactly(
                TipoNotificacion.PREGUNTA_PUBLICADA, TipoNotificacion.REVISION_APROBADA, TipoNotificacion.REVISION_RECHAZADA);
        assertThat(servicio.bandeja("rev-1", false, 1, 10).elementos()).hasSize(1);
        assertThat(servicio.bandeja("nadie", false, 1, 10).elementos()).isEmpty();
    }

    @Test
    @DisplayName("se cuentan y filtran las no leídas")
    void noLeidas() {
        variasNotificaciones();
        String primera = servicio.bandeja("autor-1", false, 1, 10).elementos().get(0).id();

        servicio.marcarComoLeida(primera, "autor-1");

        assertThat(servicio.noLeidas("autor-1")).isEqualTo(2);
        assertThat(servicio.bandeja("autor-1", true, 1, 10).elementos()).hasSize(2)
                .noneMatch(n -> n.id().equals(primera));
    }

    @Test
    @DisplayName("marcar como leída guarda la fecha y repetirlo no la cambia")
    void marcarLeida() {
        variasNotificaciones();
        String id = servicio.bandeja("autor-1", false, 1, 10).elementos().get(0).id();

        Notificacion primera = servicio.marcarComoLeida(id, "autor-1");
        reloj.avanzar(Duration.ofHours(1));
        Notificacion segunda = servicio.marcarComoLeida(id, "autor-1");

        assertThat(primera.leida()).isTrue();
        assertThat(segunda.leidaEn()).isEqualTo(primera.leidaEn());
    }

    @Test
    @DisplayName("no se puede leer la notificación de otro usuario ni una que no existe")
    void ajena() {
        variasNotificaciones();
        String id = servicio.bandeja("autor-1", false, 1, 10).elementos().get(0).id();

        assertThatThrownBy(() -> servicio.marcarComoLeida(id, "rev-1")).isInstanceOf(NotificacionNoEncontradaException.class);
        assertThatThrownBy(() -> servicio.marcarComoLeida("no-existe", "autor-1"))
                .isInstanceOf(NotificacionNoEncontradaException.class);
        assertThat(servicio.noLeidas("autor-1")).isEqualTo(3);
    }

    @Test
    @DisplayName("marcar todas como leídas afecta solo al usuario y devuelve cuántas cambiaron")
    void marcarTodas() {
        variasNotificaciones();

        assertThat(servicio.marcarTodasComoLeidas("autor-1")).isEqualTo(3);
        assertThat(servicio.marcarTodasComoLeidas("autor-1")).isZero();
        assertThat(servicio.noLeidas("autor-1")).isZero();
        assertThat(servicio.noLeidas("rev-1")).isEqualTo(1);
    }

    @Test
    @DisplayName("pedir una página que no existe devuelve la última")
    void paginaFueraDeRango() {
        variasNotificaciones();

        var pagina = servicio.bandeja("autor-1", false, 9, 2);

        assertThat(pagina.numero()).isEqualTo(2);
        assertThat(pagina.elementos()).hasSize(1);
    }

    @Test
    @DisplayName("valida los parámetros de paginación")
    void validaPaginacion() {
        assertThatThrownBy(() -> servicio.bandeja("u", false, 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.bandeja("u", false, 1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.correosEnviados(1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("los correos enviados se listan del más reciente al más antiguo")
    void correosEnviados() {
        variasNotificaciones();

        var correos = servicio.correosEnviados(1, 10);

        assertThat(correos.totalElementos()).isEqualTo(4);
        assertThat(correos.elementos().get(0).correoEnviadoEn()).isAfterOrEqualTo(correos.elementos().get(3).correoEnviadoEn());
    }
}
