package co.unicauca.bancopreguntas.history.application;

import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.EventoRecibido;
import co.unicauca.bancopreguntas.history.domain.HistorialNoEncontradoException;
import co.unicauca.bancopreguntas.history.domain.Observador;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HistoryApplicationService (casos de uso)")
class HistoryApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    private InMemoryQuestionRegistry preguntas;
    private InMemoryHistoryRepository repositorio;
    private HistoryApplicationService servicio;
    private final AtomicInteger eventos = new AtomicInteger();

    private static final Observador ADMIN = new Observador("admin-1", true, false, false);
    private static final Observador AUTOR = new Observador("autor-1", false, true, false);
    private static final Observador REVISOR = new Observador("rev-1", false, false, true);

    @BeforeEach
    void preparar() {
        preguntas = new InMemoryQuestionRegistry();
        repositorio = new InMemoryHistoryRepository(preguntas);
        AtomicInteger ids = new AtomicInteger();
        servicio = new HistoryApplicationService(repositorio, preguntas, new InMemoryProcessedEvents(),
                new RelojManual(T0.plusSeconds(1000)), () -> "h-" + ids.incrementAndGet());
    }

    private EventoRecibido evento(int segundos) {
        return new EventoRecibido("e-" + eventos.incrementAndGet(), T0.plusSeconds(segundos), "{\"k\":1}");
    }

    /** Una pregunta con todo su recorrido: envío, asignación, evaluaciones, rechazo y observación tardía. */
    private void recorrido(String q) {
        servicio.alEnviarseUnaPregunta(evento(1), q, "autor-1", "Observer", 1);
        servicio.alCambiarDeEstado(evento(2), q, "BORRADOR", "PENDIENTE_REVISION", "autor-1");
        servicio.alAsignarseRevisores(evento(3), q, 1, List.of("rev-1", "rev-2"), "admin-1", false);
        servicio.alEnviarseUnaEvaluacion(evento(4), q, 1, "rev-1", "RECHAZAR", false);
        servicio.alCompletarseUnaRevision(evento(5), q, 1, false);
        servicio.alEnviarseUnaEvaluacion(evento(6), q, 1, "rev-2", "APROBAR", true);
    }

    @Test
    @DisplayName("cada evento deja una entrada con su tipo, actor, resumen, detalles y fechas")
    void registra() {
        servicio.alEnviarseUnaPregunta(evento(1), "q-1", "autor-1", "Observer", 2);

        assertThat(repositorio.todas()).singleElement().satisfies(e -> {
            assertThat(e.tipo()).isEqualTo(TipoEntrada.PREGUNTA_ENVIADA);
            assertThat(e.preguntaId()).isEqualTo("q-1");
            assertThat(e.actorId()).isEqualTo("autor-1");
            assertThat(e.resumen()).contains("autor-1").contains("«Observer»").contains("versión 2");
            assertThat(e.detalles()).isEqualTo("{\"k\":1}");
            assertThat(e.ocurridaEn()).isEqualTo(T0.plusSeconds(1));
            assertThat(e.registradaEn()).isEqualTo(T0.plusSeconds(1000));
        });
        assertThat(preguntas.buscar("q-1")).get().satisfies(i -> assertThat(i.autorId()).isEqualTo("autor-1"));
    }

    @Test
    @DisplayName("un evento repetido no deja una segunda entrada")
    void idempotente() {
        EventoRecibido e = evento(1);

        servicio.alEnviarseUnaPregunta(e, "q-1", "autor-1", "Observer", 1);
        servicio.alEnviarseUnaPregunta(e, "q-1", "autor-1", "Observer", 1);

        assertThat(repositorio.todas()).hasSize(1);
    }

    @Test
    @DisplayName("cada tipo de evento se registra con el tipo correcto")
    void tipos() {
        String q = "q-1";
        recorrido(q);
        servicio.alReabrirseUnaPregunta(evento(7), q, "autor-1", 2);
        servicio.alPublicarseUnaPregunta(evento(8), q, "admin-1");

        assertThat(repositorio.todas()).extracting(EntradaDeHistorial::tipo).containsExactly(
                TipoEntrada.PREGUNTA_ENVIADA, TipoEntrada.CAMBIO_DE_ESTADO, TipoEntrada.REVISORES_ASIGNADOS,
                TipoEntrada.EVALUACION_ENVIADA, TipoEntrada.REVISION_COMPLETADA, TipoEntrada.EVALUACION_TARDIA,
                TipoEntrada.PREGUNTA_REABIERTA, TipoEntrada.PREGUNTA_PUBLICADA);
        assertThat(repositorio.todas()).allSatisfy(e -> assertThat(e.preguntaId()).isNotNull());
    }

    @Test
    @DisplayName("el administrador ve el rastro completo de una pregunta, en orden cronológico")
    void administrador() {
        recorrido("q-1");

        List<EntradaDeHistorial> linea = servicio.lineaDeTiempo("q-1", ADMIN);

        assertThat(linea).hasSize(6);
        assertThat(linea).extracting(EntradaDeHistorial::ocurridaEn).isSorted();
    }

    @Test
    @DisplayName("un revisor de la pregunta ve todo, incluidas las evaluaciones de sus colegas")
    void revisor() {
        recorrido("q-1");

        List<EntradaDeHistorial> linea = servicio.lineaDeTiempo("q-1", REVISOR);

        assertThat(linea).extracting(EntradaDeHistorial::tipo).contains(TipoEntrada.EVALUACION_ENVIADA,
                TipoEntrada.REVISORES_ASIGNADOS);
    }

    @Test
    @DisplayName("el autor ve el rastro de su pregunta y los resultados, pero no las evaluaciones individuales ni las asignaciones")
    void autor() {
        recorrido("q-1");

        List<EntradaDeHistorial> linea = servicio.lineaDeTiempo("q-1", AUTOR);

        assertThat(linea).extracting(EntradaDeHistorial::tipo).containsExactly(TipoEntrada.PREGUNTA_ENVIADA,
                TipoEntrada.CAMBIO_DE_ESTADO, TipoEntrada.REVISION_COMPLETADA, TipoEntrada.EVALUACION_TARDIA);
    }

    @Test
    @DisplayName("quien no es el autor ni revisor de la pregunta no la ve, y se responde como si no existiera")
    void ajeno() {
        recorrido("q-1");

        assertThatThrownBy(() -> servicio.lineaDeTiempo("q-1", new Observador("autor-2", false, true, false)))
                .isInstanceOf(HistorialNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.lineaDeTiempo("q-1", new Observador("rev-9", false, false, true)))
                .isInstanceOf(HistorialNoEncontradoException.class);
    }

    @Test
    @DisplayName("tener el rol de revisor no basta: hay que haber sido revisor de esa pregunta")
    void revisorDeOtraPregunta() {
        recorrido("q-1");
        servicio.alEnviarseUnaPregunta(evento(20), "q-2", "autor-1", "Otra", 1);

        assertThatThrownBy(() -> servicio.lineaDeTiempo("q-2", REVISOR)).isInstanceOf(HistorialNoEncontradoException.class);
    }

    @Test
    @DisplayName("una pregunta sin historial es 'no encontrada' también para el administrador")
    void desconocida() {
        assertThatThrownBy(() -> servicio.lineaDeTiempo("q-9", ADMIN)).isInstanceOf(HistorialNoEncontradoException.class);
    }

    @Test
    @DisplayName("un exrevisor sigue viendo el historial de la pregunta que revisó")
    void exRevisor() {
        recorrido("q-1");
        servicio.alReabrirseUnaPregunta(evento(30), "q-1", "autor-1", 2);
        servicio.alEnviarseUnaPregunta(evento(31), "q-1", "autor-1", "Observer", 2);

        assertThat(servicio.lineaDeTiempo("q-1", REVISOR)).hasSize(8);
    }

    @Test
    @DisplayName("'lo mío' agrupa solo lo visible de las preguntas de cada rol")
    void visiblesPara() {
        recorrido("q-1");
        servicio.alEnviarseUnaPregunta(evento(40), "q-2", "autor-2", "Ajena", 1);

        assertThat(servicio.visiblesPara(ADMIN, 1, 50).totalElementos()).isEqualTo(7);
        assertThat(servicio.visiblesPara(AUTOR, 1, 50).elementos())
                .extracting(EntradaDeHistorial::preguntaId).containsOnly("q-1").hasSize(4);
        assertThat(servicio.visiblesPara(REVISOR, 1, 50).elementos()).hasSize(6);
        assertThat(servicio.visiblesPara(new Observador("nadie", false, true, true), 1, 50).elementos()).isEmpty();
    }

    @Test
    @DisplayName("un usuario con dos roles ve la unión de lo que le corresponde")
    void dosRoles() {
        recorrido("q-1");
        servicio.alEnviarseUnaPregunta(evento(40), "q-2", "rev-1", "De rev-1 como autor", 1);

        Observador ambos = new Observador("rev-1", false, true, true);

        assertThat(servicio.visiblesPara(ambos, 1, 50).elementos()).extracting(EntradaDeHistorial::preguntaId)
                .contains("q-1", "q-2");
    }

    @Test
    @DisplayName("lo visible se devuelve de lo más reciente a lo más antiguo")
    void visiblesOrdenados() {
        recorrido("q-1");

        var pagina = servicio.visiblesPara(ADMIN, 1, 50);

        assertThat(pagina.elementos()).extracting(EntradaDeHistorial::ocurridaEn).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    @DisplayName("pedir una página que no existe devuelve la última; los parámetros se validan")
    void paginacion() {
        recorrido("q-1");

        var pagina = servicio.visiblesPara(ADMIN, 99, 4);

        assertThat(pagina.numero()).isEqualTo(2);
        assertThat(pagina.elementos()).hasSize(2);
        assertThatThrownBy(() -> servicio.visiblesPara(ADMIN, 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.visiblesPara(ADMIN, 1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("los tipos que ve el autor son los marcados como visibles para él")
    void tiposDelAutor() {
        assertThat(HistoryApplicationService.tiposVisiblesParaElAutor()).containsExactlyInAnyOrder(
                TipoEntrada.PREGUNTA_ENVIADA, TipoEntrada.PREGUNTA_REABIERTA, TipoEntrada.PREGUNTA_PUBLICADA,
                TipoEntrada.CAMBIO_DE_ESTADO, TipoEntrada.EVALUACION_TARDIA, TipoEntrada.REVISION_COMPLETADA);
    }
}
