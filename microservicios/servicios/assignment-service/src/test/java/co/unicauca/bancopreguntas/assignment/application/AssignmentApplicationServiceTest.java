package co.unicauca.bancopreguntas.assignment.application;

import co.unicauca.bancopreguntas.assignment.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.assignment.domain.Decision;
import co.unicauca.bancopreguntas.assignment.domain.EstadoRonda;
import co.unicauca.bancopreguntas.assignment.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.RevisionInvalidaException;
import co.unicauca.bancopreguntas.assignment.domain.event.ReviewersAssigned;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AssignmentApplicationService (casos de uso)")
class AssignmentApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    private InMemoryAssignmentRepository repositorio;
    private RecordingEventPublisher eventos;
    private InMemoryProcessedEvents procesados;
    private RelojManual reloj;
    private AssignmentApplicationService servicio;

    @BeforeEach
    void preparar() {
        repositorio = new InMemoryAssignmentRepository();
        eventos = new RecordingEventPublisher();
        procesados = new InMemoryProcessedEvents();
        reloj = new RelojManual(T0);
        AtomicInteger ids = new AtomicInteger();
        servicio = new AssignmentApplicationService(repositorio, eventos, procesados, reloj,
                () -> "caso-" + ids.incrementAndGet(), 5);
    }

    private void enviada(String eventId, String preguntaId, int version) {
        servicio.alEnviarseUnaPregunta(eventId, preguntaId, "autor", "Título " + preguntaId, "LECTURA_CRITICA", version);
    }

    private void enviadaYAsignada(String preguntaId, String... revisores) {
        enviada("env-" + preguntaId, preguntaId, 1);
        servicio.asignarRevisores(preguntaId, List.of(revisores), "admin");
    }

    @Test
    @DisplayName("el máximo de revisores es configurable y debe ser al menos 1")
    void maximoConfigurable() {
        assertThat(servicio.maximoDeRevisores()).isEqualTo(5);
        assertThatThrownBy(() -> new AssignmentApplicationService(repositorio, eventos, procesados, reloj, () -> "x", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("al enviarse una pregunta por primera vez se abre el caso y espera revisores, sin publicar nada")
    void primerEnvio() {
        enviada("e-1", "q-1", 1);

        var pendientes = servicio.pendientesDeAsignacion(1, 10);
        assertThat(pendientes.elementos()).hasSize(1);
        assertThat(pendientes.elementos().get(0).preguntaId()).isEqualTo("q-1");
        assertThat(pendientes.elementos().get(0).titulo()).isEqualTo("Título q-1");
        assertThat(pendientes.elementos().get(0).ronda()).isEqualTo(1);
        assertThat(eventos.publicados()).isEmpty();
    }

    @Test
    @DisplayName("un evento repetido no hace nada la segunda vez")
    void eventoRepetido() {
        enviada("e-1", "q-1", 1);
        int guardados = repositorio.guardados;

        enviada("e-1", "q-1", 1);

        assertThat(repositorio.guardados).isEqualTo(guardados);
        assertThat(servicio.pendientesDeAsignacion(1, 10).totalElementos()).isEqualTo(1);
    }

    @Test
    @DisplayName("el administrador asigna revisores y se publica review.reviewers-assigned con la versión, el título y la competencia")
    void asignar() {
        enviada("e-1", "q-1", 1);

        servicio.asignarRevisores("q-1", List.of("r1", "r2"), "admin");

        assertThat(eventos.tipos()).containsExactly("review.reviewers-assigned");
        ReviewersAssigned evento = (ReviewersAssigned) eventos.publicados().get(0);
        assertThat(evento.questionId()).isEqualTo("q-1");
        assertThat(evento.authorId()).isEqualTo("autor");
        assertThat(evento.round()).isEqualTo(1);
        assertThat(evento.reviewerIds()).containsExactly("r1", "r2");
        assertThat(evento.assignedBy()).isEqualTo("admin");
        assertThat(evento.automatic()).isFalse();
        assertThat(evento.questionVersion()).isEqualTo(1);
        assertThat(evento.title()).isEqualTo("Título q-1");
        assertThat(evento.competency()).isEqualTo("LECTURA_CRITICA");
        assertThat(servicio.pendientesDeAsignacion(1, 10).totalElementos()).isZero();
    }

    @Test
    @DisplayName("agregar un revisor publica solo al nuevo")
    void agregarRevisor() {
        enviadaYAsignada("q-1", "r1");

        servicio.asignarRevisores("q-1", List.of("r2"), "admin");

        ReviewersAssigned segundo = (ReviewersAssigned) eventos.publicados().get(1);
        assertThat(segundo.reviewerIds()).containsExactly("r2");
    }

    @Test
    @DisplayName("una asignación inválida no publica nada ni cambia el caso")
    void asignacionInvalida() {
        enviada("e-1", "q-1", 1);

        assertThatThrownBy(() -> servicio.asignarRevisores("q-1", List.of("autor"), "admin"))
                .isInstanceOf(RevisionInvalidaException.class);

        assertThat(eventos.publicados()).isEmpty();
        assertThat(servicio.pendientesDeAsignacion(1, 10).totalElementos()).isEqualTo(1);
    }

    @Test
    @DisplayName("asignar a una pregunta sin revisión es 'no encontrada'")
    void asignarSinCaso() {
        assertThatThrownBy(() -> servicio.asignarRevisores("nada", List.of("r1"), "admin"))
                .isInstanceOf(CasoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.caso("nada")).isInstanceOf(CasoNoEncontradoException.class);
    }

    @Test
    @DisplayName("tras un rechazo y la reapertura, el reenvío reasigna solo a los mismos revisores y lo publica")
    void reenvioReasignaSolo() {
        enviadaYAsignada("q-1", "r1", "r2");
        servicio.alCompletarseLaRevision("fin-1", "q-1", 1, false);
        servicio.alReabrirseUnaPregunta("reab-1", "q-1");
        reloj.avanzar(Duration.ofHours(1));

        enviada("env-2", "q-1", 2);

        ReviewersAssigned reasignados = (ReviewersAssigned) eventos.publicados().getLast();
        assertThat(reasignados.reviewerIds()).containsExactly("r1", "r2");
        assertThat(reasignados.automatic()).isTrue();
        assertThat(reasignados.assignedBy()).isEqualTo("system");
        assertThat(reasignados.round()).isEqualTo(2);
        assertThat(reasignados.questionVersion()).isEqualTo(2);
        // no vuelve a la lista del administrador: ya tiene sus revisores
        assertThat(servicio.pendientesDeAsignacion(1, 10).totalElementos()).isZero();
        assertThat(servicio.asignadasA("r2", FiltroAsignacion.PENDIENTES, 1, 10).elementos())
                .extracting(a -> a.ronda()).containsExactly(2);
    }

    @Test
    @DisplayName("el reenvío sin que haya llegado la reapertura también funciona")
    void reenvioSinReapertura() {
        enviadaYAsignada("q-1", "r1");
        servicio.alCompletarseLaRevision("fin-1", "q-1", 1, false);

        enviada("env-2", "q-1", 2);

        assertThat(eventos.publicados().getLast()).isInstanceOf(ReviewersAssigned.class);
    }

    @Test
    @DisplayName("una reapertura cierra la ronda rechazada")
    void reaperturaCierra() {
        enviadaYAsignada("q-1", "r1", "r2");
        servicio.alCompletarseLaRevision("fin-1", "q-1", 1, false);

        servicio.alReabrirseUnaPregunta("reab-1", "q-1");

        assertThat(servicio.caso("q-1").rondas().get(0).estado()).isEqualTo(EstadoRonda.CERRADA);
        assertThat(servicio.asignadasA("r2", FiltroAsignacion.PENDIENTES, 1, 10).elementos()).isEmpty();
    }

    @Test
    @DisplayName("una reapertura repetida o de una pregunta desconocida se ignora")
    void reaperturaIgnorada() {
        servicio.alReabrirseUnaPregunta("reab-x", "no-existe");
        enviadaYAsignada("q-1", "r1");
        servicio.alCompletarseLaRevision("fin-1", "q-1", 1, false);
        servicio.alReabrirseUnaPregunta("reab-1", "q-1");
        int guardados = repositorio.guardados;

        servicio.alReabrirseUnaPregunta("reab-1", "q-1");
        servicio.alReabrirseUnaPregunta("reab-2", "q-1"); // ya estaba cerrada

        assertThat(repositorio.guardados).isEqualTo(guardados);
    }

    @Test
    @DisplayName("un envío cuando la ronda sigue abierta se ignora sin fallar")
    void envioConRondaAbierta() {
        enviadaYAsignada("q-1", "r1");

        enviada("env-raro", "q-1", 2);

        assertThat(servicio.caso("q-1").rondas()).hasSize(1);
    }

    @Test
    @DisplayName("review.evaluation-submitted: la bandeja separa pendientes y evaluadas")
    void bandeja() {
        enviadaYAsignada("q-1", "r1");
        reloj.avanzar(Duration.ofMinutes(5));
        enviadaYAsignada("q-2", "r1", "r2");
        servicio.alEvaluarseUnaPregunta("ev-1", "q-1", 1, "r1", Decision.APROBAR);

        assertThat(servicio.asignadasA("r1", FiltroAsignacion.PENDIENTES, 1, 10).elementos())
                .extracting(a -> a.preguntaId()).containsExactly("q-2");
        assertThat(servicio.asignadasA("r1", FiltroAsignacion.EVALUADAS, 1, 10).elementos())
                .extracting(a -> a.preguntaId(), a -> a.miDecision()).containsExactly(
                        org.assertj.core.groups.Tuple.tuple("q-1", Decision.APROBAR));
        assertThat(servicio.asignadasA("r1", FiltroAsignacion.TODAS, 1, 10).elementos())
                .extracting(a -> a.preguntaId()).containsExactly("q-2", "q-1");
        assertThat(servicio.asignadasA("r1", null, 1, 10).elementos()).hasSize(1); // por defecto: pendientes
        assertThat(servicio.asignadasA("r2", FiltroAsignacion.PENDIENTES, 1, 10).elementos()).hasSize(1);
    }

    @Test
    @DisplayName("una evaluación repetida o de una pregunta desconocida se ignora")
    void evaluacionIgnorada() {
        servicio.alEvaluarseUnaPregunta("ev-x", "no-existe", 1, "r1", Decision.APROBAR);
        enviadaYAsignada("q-1", "r1");
        servicio.alEvaluarseUnaPregunta("ev-1", "q-1", 1, "r1", Decision.APROBAR);
        int guardados = repositorio.guardados;

        servicio.alEvaluarseUnaPregunta("ev-1", "q-1", 1, "r1", Decision.RECHAZAR);
        servicio.alEvaluarseUnaPregunta("ev-2", "q-1", 1, "intruso", Decision.APROBAR);

        assertThat(repositorio.guardados).isEqualTo(guardados);
        assertThat(servicio.asignadasA("r1", FiltroAsignacion.EVALUADAS, 1, 10).elementos())
                .extracting(a -> a.miDecision()).containsExactly(Decision.APROBAR);
    }

    @Test
    @DisplayName("review.completed: una pregunta rechazada sigue pendiente para el revisor que no evaluó (observación tardía)")
    void pendienteTrasRechazo() {
        enviadaYAsignada("q-1", "r1", "r2");
        servicio.alEvaluarseUnaPregunta("ev-1", "q-1", 1, "r1", Decision.RECHAZAR);
        servicio.alCompletarseLaRevision("fin-1", "q-1", 1, false);

        assertThat(servicio.asignadasA("r2", FiltroAsignacion.PENDIENTES, 1, 10).elementos())
                .singleElement().satisfies(a -> {
                    assertThat(a.estadoRonda()).isEqualTo(EstadoRonda.RECHAZADA);
                    assertThat(a.pendiente()).isTrue();
                });
    }

    @Test
    @DisplayName("review.completed: una ronda decidida ya no admite revisores y un aviso repetido no cambia nada")
    void rondaDecidida() {
        enviadaYAsignada("q-1", "r1");
        servicio.alCompletarseLaRevision("fin-1", "q-1", 1, true);
        int guardados = repositorio.guardados;

        servicio.alCompletarseLaRevision("fin-1", "q-1", 1, false);
        servicio.alCompletarseLaRevision("fin-x", "no-existe", 1, true);

        assertThat(repositorio.guardados).isEqualTo(guardados);
        assertThat(servicio.caso("q-1").rondas().get(0).estado()).isEqualTo(EstadoRonda.APROBADA);
        assertThatThrownBy(() -> servicio.asignarRevisores("q-1", List.of("r2"), "admin"))
                .isInstanceOf(co.unicauca.bancopreguntas.assignment.domain.OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("pedir una página que no existe devuelve la última")
    void paginaFueraDeRango() {
        for (int i = 1; i <= 3; i++) {
            enviada("e-" + i, "q-" + i, 1);
            reloj.avanzar(Duration.ofMinutes(1));
        }

        var pagina = servicio.pendientesDeAsignacion(9, 2);

        assertThat(pagina.numero()).isEqualTo(2);
        assertThat(pagina.elementos()).hasSize(1);
        assertThat(pagina.totalElementos()).isEqualTo(3);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("valida los parámetros de paginación y el revisor")
    void validaParametros() {
        assertThatThrownBy(() -> servicio.pendientesDeAsignacion(0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.pendientesDeAsignacion(1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.asignadasA(" ", FiltroAsignacion.TODAS, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
