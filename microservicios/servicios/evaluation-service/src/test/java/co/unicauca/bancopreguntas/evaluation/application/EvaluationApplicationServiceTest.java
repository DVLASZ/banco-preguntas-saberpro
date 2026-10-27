package co.unicauca.bancopreguntas.evaluation.application;

import co.unicauca.bancopreguntas.evaluation.domain.AccesoNoPermitidoException;
import co.unicauca.bancopreguntas.evaluation.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.evaluation.domain.Decision;
import co.unicauca.bancopreguntas.evaluation.domain.EstadoRonda;
import co.unicauca.bancopreguntas.evaluation.domain.Observador;
import co.unicauca.bancopreguntas.evaluation.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.evaluation.domain.ResultadoDeDecision;
import co.unicauca.bancopreguntas.evaluation.domain.RevisionInvalidaException;
import co.unicauca.bancopreguntas.evaluation.domain.Unanimidad;
import co.unicauca.bancopreguntas.evaluation.domain.event.EvaluationSubmitted;
import co.unicauca.bancopreguntas.evaluation.domain.event.ReviewCompleted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EvaluationApplicationService (casos de uso)")
class EvaluationApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    private InMemoryEvaluationRepository repositorio;
    private RecordingEventPublisher eventos;
    private InMemoryProcessedEvents procesados;
    private RelojManual reloj;
    private EvaluationApplicationService servicio;
    private final AtomicInteger consecutivo = new AtomicInteger();

    @BeforeEach
    void preparar() {
        repositorio = new InMemoryEvaluationRepository();
        eventos = new RecordingEventPublisher();
        procesados = new InMemoryProcessedEvents();
        reloj = new RelojManual(T0);
        servicio = new EvaluationApplicationService(repositorio, eventos, procesados, new Unanimidad(), reloj,
                () -> "caso-" + consecutivo.incrementAndGet());
    }

    private void enviada(String preguntaId, int version) {
        servicio.alEnviarseUnaPregunta("env-" + preguntaId + "-" + version, preguntaId, "autor", "Título " + preguntaId,
                "LECTURA_CRITICA", version);
    }

    private void asignada(String preguntaId, int version, int ronda, boolean automatica, String... revisores) {
        servicio.alAsignarseRevisores("asig-" + consecutivo.incrementAndGet(), preguntaId, "autor", "Título " + preguntaId,
                "LECTURA_CRITICA", version, ronda, List.of(revisores), automatica ? "system" : "admin", automatica, T0);
    }

    private void enviadaYAsignada(String preguntaId, String... revisores) {
        enviada(preguntaId, 1);
        asignada(preguntaId, 1, 1, false, revisores);
    }

    private EstadoRonda estadoDeLaRonda(String preguntaId, int ronda) {
        return servicio.caso(preguntaId, new Observador("admin", true)).rondas().get(ronda - 1).estado();
    }

    // ---------------------------------------------------------------- eventos de entrada

    @Test
    @DisplayName("al enviarse una pregunta se abre el caso con una ronda pendiente, sin publicar nada")
    void primerEnvio() {
        enviada("q-1", 1);

        var vista = servicio.caso("q-1", new Observador("admin", true));
        assertThat(vista.titulo()).isEqualTo("Título q-1");
        assertThat(vista.rondas()).singleElement().satisfies(r -> {
            assertThat(r.estado()).isEqualTo(EstadoRonda.PENDIENTE_ASIGNACION);
            assertThat(r.totalDeRevisores()).isZero();
        });
        assertThat(eventos.publicados()).isEmpty();
    }

    @Test
    @DisplayName("un evento repetido no hace nada la segunda vez")
    void eventoRepetido() {
        servicio.alEnviarseUnaPregunta("e-1", "q-1", "autor", "T", "INGLES", 1);
        int guardados = repositorio.guardados;

        servicio.alEnviarseUnaPregunta("e-1", "q-1", "autor", "T", "INGLES", 1);

        assertThat(repositorio.guardados).isEqualTo(guardados);
    }

    @Test
    @DisplayName("al asignarse revisores la ronda queda en curso y ellos pueden evaluar")
    void revisoresAsignados() {
        enviadaYAsignada("q-1", "r1", "r2");

        var ronda = servicio.caso("q-1", new Observador("admin", true)).rondas().get(0);
        assertThat(ronda.estado()).isEqualTo(EstadoRonda.EN_CURSO);
        assertThat(ronda.revisores()).extracting(r -> r.revisorId()).containsExactly("r1", "r2");
        assertThat(ronda.revisores().get(0).asignadoPor()).isEqualTo("admin");
        assertThat(servicio.evaluar("q-1", "r1", Decision.APROBAR, null).decision()).isEqualTo(ResultadoDeDecision.PENDIENTE);
    }

    @Test
    @DisplayName("agregar un revisor a una ronda en curso suma a los anteriores, y un aviso repetido no duplica")
    void agregarRevisor() {
        enviadaYAsignada("q-1", "r1");
        asignada("q-1", 1, 1, false, "r2");
        servicio.alAsignarseRevisores("asig-repetido", "q-1", "autor", "T", "INGLES", 1, 1, List.of("r3"), "admin", false, T0);
        servicio.alAsignarseRevisores("asig-repetido", "q-1", "autor", "T", "INGLES", 1, 1, List.of("r3"), "admin", false, T0);

        assertThat(servicio.caso("q-1", new Observador("admin", true)).rondas().get(0).revisores())
                .extracting(r -> r.revisorId()).containsExactly("r1", "r2", "r3");
    }

    @Test
    @DisplayName("si la asignación llega antes que el aviso del envío, la ronda se abre con los datos del propio evento")
    void asignacionAntesDelEnvio() {
        asignada("q-1", 1, 1, true, "r1");

        assertThat(estadoDeLaRonda("q-1", 1)).isEqualTo(EstadoRonda.EN_CURSO);
        int guardados = repositorio.guardados;

        enviada("q-1", 1); // el aviso atrasado no abre otra ronda

        assertThat(repositorio.guardados).isEqualTo(guardados);
        assertThat(servicio.caso("q-1", new Observador("admin", true)).rondas()).hasSize(1);
    }

    @Test
    @DisplayName("una asignación a una ronda inexistente o ya decidida se ignora sin fallar")
    void asignacionIgnorada() {
        enviadaYAsignada("q-1", "r1");
        servicio.evaluar("q-1", "r1", Decision.RECHAZAR, "Mal");
        int guardados = repositorio.guardados;

        asignada("q-1", 1, 1, false, "r9");
        asignada("q-1", 1, 7, false, "r9");

        assertThat(repositorio.guardados).isEqualTo(guardados);
    }

    // ---------------------------------------------------------------- evaluar

    @Test
    @DisplayName("aprobar con todos publica las evaluaciones y review.completed APPROVED")
    void aprobacion() {
        enviadaYAsignada("q-1", "r1", "r2");

        servicio.evaluar("q-1", "r1", Decision.APROBAR, null);
        var resultado = servicio.evaluar("q-1", "r2", Decision.APROBAR, "Muy buena");

        assertThat(resultado.decision()).isEqualTo(ResultadoDeDecision.APROBADA);
        assertThat(eventos.tipos()).containsExactly("review.evaluation-submitted", "review.evaluation-submitted",
                "review.completed");
        ReviewCompleted completado = (ReviewCompleted) eventos.publicados().get(2);
        assertThat(completado.result()).isEqualTo(ReviewCompleted.APPROVED);
        assertThat(completado.observations()).hasSize(2);
        assertThat(completado.round()).isEqualTo(1);
        assertThat(estadoDeLaRonda("q-1", 1)).isEqualTo(EstadoRonda.APROBADA);
    }

    @Test
    @DisplayName("el primer rechazo publica review.completed REJECTED con la observación")
    void rechazo() {
        enviadaYAsignada("q-1", "r1", "r2");

        servicio.evaluar("q-1", "r1", Decision.RECHAZAR, "Hay dos respuestas correctas");

        ReviewCompleted completado = (ReviewCompleted) eventos.publicados().get(1);
        assertThat(completado.result()).isEqualTo(ReviewCompleted.REJECTED);
        assertThat(completado.observations()).singleElement().satisfies(o -> {
            assertThat(o.reviewerId()).isEqualTo("r1");
            assertThat(o.decision()).isEqualTo("RECHAZAR");
            assertThat(o.text()).isEqualTo("Hay dos respuestas correctas");
        });
    }

    @Test
    @DisplayName("la observación tardía se publica como evaluación tardía y no repite review.completed")
    void observacionTardia() {
        enviadaYAsignada("q-1", "r1", "r2");
        servicio.evaluar("q-1", "r1", Decision.RECHAZAR, "Mal");

        servicio.evaluar("q-1", "r2", Decision.RECHAZAR, "Tampoco me convence");

        assertThat(eventos.tipos()).containsExactly("review.evaluation-submitted", "review.completed",
                "review.evaluation-submitted");
        EvaluationSubmitted tardia = (EvaluationSubmitted) eventos.publicados().get(2);
        assertThat(tardia.late()).isTrue();
        assertThat(tardia.reviewerId()).isEqualTo("r2");
    }

    @Test
    @DisplayName("una evaluación inválida no publica nada")
    void evaluacionInvalida() {
        enviadaYAsignada("q-1", "r1");

        assertThatThrownBy(() -> servicio.evaluar("q-1", "r1", Decision.RECHAZAR, " "))
                .isInstanceOf(RevisionInvalidaException.class);
        assertThatThrownBy(() -> servicio.evaluar("q-1", "otro", Decision.APROBAR, null))
                .isInstanceOf(AccesoNoPermitidoException.class);

        assertThat(eventos.publicados()).isEmpty();
    }

    @Test
    @DisplayName("no se evalúa antes de que haya revisores asignados")
    void sinRevisores() {
        enviada("q-1", 1);

        assertThatThrownBy(() -> servicio.evaluar("q-1", "r1", Decision.APROBAR, null))
                .isInstanceOf(AccesoNoPermitidoException.class);
    }

    @Test
    @DisplayName("evaluar dos veces es una operación no permitida")
    void evaluarDosVeces() {
        enviadaYAsignada("q-1", "r1", "r2");
        servicio.evaluar("q-1", "r1", Decision.APROBAR, null);

        assertThatThrownBy(() -> servicio.evaluar("q-1", "r1", Decision.APROBAR, null))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("evaluar una pregunta sin revisión es 'no encontrada'")
    void evaluarSinCaso() {
        assertThatThrownBy(() -> servicio.evaluar("nada", "r1", Decision.APROBAR, null))
                .isInstanceOf(CasoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.caso("nada", new Observador("admin", true)))
                .isInstanceOf(CasoNoEncontradoException.class);
    }

    // ---------------------------------------------------------------- reapertura y rondas nuevas

    @Test
    @DisplayName("una reapertura cierra la ronda rechazada y las observaciones dejan de admitirse")
    void reaperturaCierra() {
        enviadaYAsignada("q-1", "r1", "r2");
        servicio.evaluar("q-1", "r1", Decision.RECHAZAR, "Mal");

        servicio.alReabrirseUnaPregunta("reab-1", "q-1");

        assertThat(estadoDeLaRonda("q-1", 1)).isEqualTo(EstadoRonda.CERRADA);
        assertThatThrownBy(() -> servicio.evaluar("q-1", "r2", Decision.APROBAR, null))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("una reapertura repetida o de una pregunta desconocida se ignora")
    void reaperturaIgnorada() {
        servicio.alReabrirseUnaPregunta("reab-x", "no-existe");
        enviadaYAsignada("q-1", "r1");
        servicio.evaluar("q-1", "r1", Decision.RECHAZAR, "Mal");
        servicio.alReabrirseUnaPregunta("reab-1", "q-1");
        int guardados = repositorio.guardados;

        servicio.alReabrirseUnaPregunta("reab-1", "q-1");
        servicio.alReabrirseUnaPregunta("reab-2", "q-1"); // ya estaba cerrada

        assertThat(repositorio.guardados).isEqualTo(guardados);
    }

    @Test
    @DisplayName("tras el rechazo y la reapertura, en la ronda nueva los mismos revisores evalúan de nuevo")
    void rondaNueva() {
        enviadaYAsignada("q-1", "r1", "r2");
        servicio.evaluar("q-1", "r1", Decision.RECHAZAR, "Mal");
        servicio.alReabrirseUnaPregunta("reab-1", "q-1");

        enviada("q-1", 2);
        asignada("q-1", 2, 2, true, "r1", "r2");
        servicio.evaluar("q-1", "r1", Decision.APROBAR, null);
        var resultado = servicio.evaluar("q-1", "r2", Decision.APROBAR, null);

        assertThat(resultado.decision()).isEqualTo(ResultadoDeDecision.APROBADA);
        assertThat(resultado.ronda().numero()).isEqualTo(2);
        assertThat(estadoDeLaRonda("q-1", 1)).isEqualTo(EstadoRonda.CERRADA);
    }

    @Test
    @DisplayName("un envío cuando la ronda sigue abierta se ignora sin fallar")
    void envioConRondaAbierta() {
        enviadaYAsignada("q-1", "r1");

        enviada("q-1", 2);

        assertThat(servicio.caso("q-1", new Observador("admin", true)).rondas()).hasSize(1);
    }

    // ---------------------------------------------------------------- consulta según quién mira

    @Test
    @DisplayName("el autor ve el detalle de la ronda rechazada con las observaciones de los revisores")
    void autorLeeLasObservaciones() {
        enviadaYAsignada("q-1", "r1", "r2");
        servicio.evaluar("q-1", "r1", Decision.RECHAZAR, "Falta contexto");
        servicio.evaluar("q-1", "r2", Decision.APROBAR, "Por mí está bien");

        var ronda = servicio.caso("q-1", new Observador("autor", false)).rondas().get(0);

        assertThat(ronda.detalleVisible()).isTrue();
        assertThat(ronda.revisores()).extracting(r -> r.evaluacion().observacion())
                .containsExactly("Falta contexto", "Por mí está bien");
    }

    @Test
    @DisplayName("el autor no ve el detalle mientras la ronda está en curso y un ajeno no ve nada")
    void quienVeQue() {
        enviadaYAsignada("q-1", "r1", "r2");

        assertThat(servicio.caso("q-1", new Observador("autor", false)).rondas().get(0).detalleVisible()).isFalse();
        assertThat(servicio.caso("q-1", new Observador("r1", false)).rondas().get(0).detalleVisible()).isTrue();
        assertThatThrownBy(() -> servicio.caso("q-1", new Observador("intruso", false)))
                .isInstanceOf(AccesoNoPermitidoException.class);
    }
}
