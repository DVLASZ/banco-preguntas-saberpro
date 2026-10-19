package co.unicauca.bancopreguntas.simulation.application;

import co.unicauca.bancopreguntas.simulation.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.simulation.domain.CriteriosDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.CriteriosInvalidosException;
import co.unicauca.bancopreguntas.simulation.domain.EstadoSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.simulation.domain.PreguntaDeCatalogo;
import co.unicauca.bancopreguntas.simulation.domain.Preguntas;
import co.unicauca.bancopreguntas.simulation.domain.PreguntasInsuficientesException;
import co.unicauca.bancopreguntas.simulation.domain.SeleccionAleatoria;
import co.unicauca.bancopreguntas.simulation.domain.SimulacroNoEncontradoException;
import co.unicauca.bancopreguntas.simulation.domain.TiempoAgotadoException;
import co.unicauca.bancopreguntas.simulation.domain.event.SimulationCompleted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SimulationApplicationService (casos de uso)")
class SimulationApplicationServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-16T10:00:00Z");

    private InMemoryCatalogRepository catalogo;
    private InMemorySimulacroRepository simulacros;
    private RecordingEventPublisher eventos;
    private RelojManual reloj;
    private SimulationApplicationService servicio;

    @BeforeEach
    void preparar() {
        catalogo = new InMemoryCatalogRepository();
        simulacros = new InMemorySimulacroRepository();
        eventos = new RecordingEventPublisher();
        reloj = new RelojManual(T0);
        AtomicInteger ids = new AtomicInteger();
        servicio = new SimulationApplicationService(catalogo, simulacros, eventos, new InMemoryProcessedEvents(),
                new SeleccionAleatoria(new Random(42)), reloj, () -> "s-" + ids.incrementAndGet(), 10);
        Preguntas.lectura(5).forEach(catalogo::guardar);
        catalogo.guardar(Preguntas.pregunta("i-1", "INGLES", "AVANZADO", "Verbos", 'C'));
        catalogo.guardar(Preguntas.pregunta("i-2", "INGLES", "BASICO", "Verbos", 'D'));
    }

    private VistaDeSimulacro iniciar(int cantidad) {
        return servicio.iniciar("estudiante1", Preguntas.criterios(cantidad, 30));
    }

    // ---------------------------------------------------------------- catálogo

    @Test
    @DisplayName("una pregunta publicada entra al catálogo y un evento repetido no la duplica")
    void alPublicar() {
        servicio.alPublicarseUnaPregunta("e-1", Preguntas.pregunta("n-1"));
        servicio.alPublicarseUnaPregunta("e-1", Preguntas.pregunta("n-2"));

        assertThat(catalogo.buscar("n-1")).isPresent();
        assertThat(catalogo.buscar("n-2")).isEmpty();
    }

    @Test
    @DisplayName("una versión más nueva reemplaza la copia y una más vieja se ignora")
    void versiones() {
        PreguntaDeCatalogo v2 = new PreguntaDeCatalogo("q-1", 2, "t", "LECTURA_CRITICA", "tema", "s", "BASICO", "c",
                "Enunciado nuevo", "a", "b", "c", "d", 'A', T0, true);
        PreguntaDeCatalogo v1 = new PreguntaDeCatalogo("q-1", 1, "t", "LECTURA_CRITICA", "tema", "s", "BASICO", "c",
                "Enunciado viejo", "a", "b", "c", "d", 'A', T0, true);

        servicio.alPublicarseUnaPregunta("e-2", v2);
        servicio.alPublicarseUnaPregunta("e-3", v1);

        assertThat(catalogo.buscar("q-1").orElseThrow().enunciado()).isEqualTo("Enunciado nuevo");
    }

    @Test
    @DisplayName("una pregunta archivada sale del catálogo; otros cambios de estado no la tocan")
    void alArchivar() {
        servicio.alCambiarElEstadoDeUnaPregunta("e-1", "q-1", "PUBLICADA");
        assertThat(catalogo.buscar("q-1").orElseThrow().disponible()).isTrue();

        servicio.alCambiarElEstadoDeUnaPregunta("e-2", "q-1", "ARCHIVADA");

        assertThat(catalogo.buscar("q-1").orElseThrow().disponible()).isFalse();
        assertThat(catalogo.disponibles()).extracting(PreguntaDeCatalogo::preguntaId).doesNotContain("q-1");
    }

    @Test
    @DisplayName("archivar una pregunta que no se conoce no falla")
    void archivarDesconocida() {
        servicio.alCambiarElEstadoDeUnaPregunta("e-1", "no-existe", "ARCHIVADA");

        assertThat(catalogo.disponibles()).hasSize(7);
    }

    @Test
    @DisplayName("el resumen cuenta las preguntas disponibles por competencia y dificultad")
    void resumen() {
        var resumen = servicio.resumenDelCatalogo();

        assertThat(resumen.total()).isEqualTo(7);
        assertThat(resumen.porCompetencia()).containsEntry("LECTURA_CRITICA", 5).containsEntry("INGLES", 2);
        assertThat(resumen.porDificultad()).containsEntry("BASICO", 6).containsEntry("AVANZADO", 1);
    }

    // ---------------------------------------------------------------- generar

    @Test
    @DisplayName("genera un simulacro con la cantidad pedida y el tiempo corriendo")
    void iniciarSimulacro() {
        VistaDeSimulacro vista = iniciar(4);

        assertThat(vista.simulacro().getEstado()).isEqualTo(EstadoSimulacro.EN_CURSO);
        assertThat(vista.simulacro().getItems()).hasSize(4);
        assertThat(vista.segundosRestantes()).isEqualTo(1800);
        assertThat(simulacros.buscar(vista.simulacro().getId())).isPresent();
    }

    @Test
    @DisplayName("solo usa preguntas que cumplen los criterios")
    void conFiltros() {
        VistaDeSimulacro vista = servicio.iniciar("estudiante1",
                new CriteriosDeSimulacro(Set.of("INGLES"), null, null, 2, 15));

        assertThat(vista.simulacro().getItems()).extracting(i -> i.pregunta().competencia()).containsOnly("INGLES");
    }

    @Test
    @DisplayName("si faltan preguntas que cumplan los criterios responde cuántas hay")
    void insuficientes() {
        assertThatThrownBy(() -> servicio.iniciar("estudiante1", new CriteriosDeSimulacro(Set.of("INGLES"), null, null, 5, 15)))
                .isInstanceOfSatisfying(PreguntasInsuficientesException.class, e -> {
                    assertThat(e.disponibles()).isEqualTo(2);
                    assertThat(e.solicitadas()).isEqualTo(5);
                });
    }

    @Test
    @DisplayName("una pregunta archivada no entra en simulacros nuevos")
    void archivadasNoEntran() {
        servicio.alCambiarElEstadoDeUnaPregunta("e-1", "i-1", "ARCHIVADA");
        servicio.alCambiarElEstadoDeUnaPregunta("e-2", "i-2", "ARCHIVADA");

        assertThatThrownBy(() -> servicio.iniciar("estudiante1", new CriteriosDeSimulacro(Set.of("INGLES"), null, null, 1, 15)))
                .isInstanceOf(PreguntasInsuficientesException.class);
    }

    @Test
    @DisplayName("no permite pedir más preguntas que el máximo configurado")
    void maximo() {
        assertThatThrownBy(() -> iniciar(11)).isInstanceOf(CriteriosInvalidosException.class).hasMessageContaining("10");
    }

    // ---------------------------------------------------------------- responder y finalizar

    @Test
    @DisplayName("responder guarda la respuesta y descuenta el tiempo")
    void responder() {
        VistaDeSimulacro vista = iniciar(3);
        String id = vista.simulacro().getId();
        String pregunta = vista.simulacro().getItems().get(0).pregunta().preguntaId();
        reloj.avanzar(Duration.ofMinutes(10));

        VistaDeSimulacro despues = servicio.responder(id, "estudiante1", pregunta, 'B');

        assertThat(despues.simulacro().respondidas()).isEqualTo(1);
        assertThat(despues.segundosRestantes()).isEqualTo(1200);
    }

    @Test
    @DisplayName("finalizar califica, revela las respuestas y publica simulation.completed")
    void finalizar() {
        VistaDeSimulacro vista = servicio.iniciar("estudiante1", new CriteriosDeSimulacro(Set.of("LECTURA_CRITICA"), null, null, 3, 30));
        String id = vista.simulacro().getId();
        vista.simulacro().getItems().forEach(i -> servicio.responder(id, "estudiante1", i.pregunta().preguntaId(), 'B'));
        reloj.avanzar(Duration.ofMinutes(12));

        VistaDeSimulacro cerrada = servicio.finalizar(id, "estudiante1");

        assertThat(cerrada.simulacro().getEstado()).isEqualTo(EstadoSimulacro.FINALIZADO);
        assertThat(cerrada.simulacro().getResultado().puntaje()).isEqualTo(100.0);
        assertThat(cerrada.segundosRestantes()).isZero();
        assertThat(eventos.publicados()).singleElement().isInstanceOfSatisfying(SimulationCompleted.class, e -> {
            assertThat(e.simulationId()).isEqualTo(id);
            assertThat(e.studentId()).isEqualTo("estudiante1");
            assertThat(e.expired()).isFalse();
            assertThat(e.durationSeconds()).isEqualTo(720);
            assertThat(e.total()).isEqualTo(3);
            assertThat(e.correct()).isEqualTo(3);
            assertThat(e.score()).isEqualTo(100.0);
            assertThat(e.byCompetency()).containsExactly(new SimulationCompleted.ByCompetency("LECTURA_CRITICA", 3, 3));
        });
    }

    @Test
    @DisplayName("finalizar dos veces no se permite y no publica otra vez")
    void finalizarDosVeces() {
        String id = iniciar(2).simulacro().getId();
        servicio.finalizar(id, "estudiante1");

        assertThatThrownBy(() -> servicio.finalizar(id, "estudiante1")).isInstanceOf(OperacionNoPermitidaException.class);
        assertThat(eventos.publicados()).hasSize(1);
    }

    // ---------------------------------------------------------------- tiempo

    @Test
    @DisplayName("responder con el tiempo agotado cierra y califica el simulacro y rechaza la respuesta")
    void responderVencido() {
        VistaDeSimulacro vista = iniciar(3);
        String id = vista.simulacro().getId();
        var items = vista.simulacro().getItems();
        servicio.responder(id, "estudiante1", items.get(0).pregunta().preguntaId(), 'B');
        reloj.avanzar(Duration.ofMinutes(31));

        assertThatThrownBy(() -> servicio.responder(id, "estudiante1", items.get(1).pregunta().preguntaId(), 'B'))
                .isInstanceOf(TiempoAgotadoException.class);

        assertThat(simulacros.buscar(id).orElseThrow().getEstado()).isEqualTo(EstadoSimulacro.EXPIRADO);
        assertThat(eventos.publicados()).singleElement().isInstanceOfSatisfying(SimulationCompleted.class, e -> {
            assertThat(e.expired()).isTrue();
            assertThat(e.correct()).isEqualTo(1);
            assertThat(e.unanswered()).isEqualTo(2);
            assertThat(e.durationSeconds()).isEqualTo(1800);
        });
    }

    @Test
    @DisplayName("consultar un simulacro vencido lo cierra en ese momento")
    void consultarVencido() {
        String id = iniciar(2).simulacro().getId();
        reloj.avanzar(Duration.ofHours(1));

        VistaDeSimulacro vista = servicio.obtener(id, "estudiante1", false);

        assertThat(vista.simulacro().getEstado()).isEqualTo(EstadoSimulacro.EXPIRADO);
        assertThat(vista.segundosRestantes()).isZero();
        assertThat(eventos.publicados()).hasSize(1);
    }

    @Test
    @DisplayName("el cierre periódico califica los vencidos aunque nadie vuelva, y solo una vez")
    void cerrarVencidos() {
        String vencido = iniciar(2).simulacro().getId();
        reloj.avanzar(Duration.ofMinutes(20));
        String vigente = iniciar(2).simulacro().getId();
        reloj.avanzar(Duration.ofMinutes(15));

        assertThat(servicio.cerrarVencidos()).isEqualTo(1);
        assertThat(servicio.cerrarVencidos()).isZero();

        assertThat(simulacros.buscar(vencido).orElseThrow().getEstado()).isEqualTo(EstadoSimulacro.EXPIRADO);
        assertThat(simulacros.buscar(vigente).orElseThrow().getEstado()).isEqualTo(EstadoSimulacro.EN_CURSO);
        assertThat(eventos.publicados()).hasSize(1);
    }

    // ---------------------------------------------------------------- acceso

    @Test
    @DisplayName("un estudiante no consulta, contesta ni finaliza el simulacro de otro")
    void ajeno() {
        VistaDeSimulacro vista = iniciar(2);
        String id = vista.simulacro().getId();
        String pregunta = vista.simulacro().getItems().get(0).pregunta().preguntaId();

        assertThatThrownBy(() -> servicio.obtener(id, "estudiante2", false)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> servicio.responder(id, "estudiante2", pregunta, 'A')).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> servicio.finalizar(id, "estudiante2")).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("docentes y administradores pueden consultar cualquier simulacro")
    void docentePuedeVerTodos() {
        String id = iniciar(2).simulacro().getId();

        assertThat(servicio.obtener(id, "docente1", true).simulacro().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("un simulacro que no existe responde no encontrado")
    void noExiste() {
        assertThatThrownBy(() -> servicio.obtener("no-existe", "estudiante1", false)).isInstanceOf(SimulacroNoEncontradoException.class);
    }

    // ---------------------------------------------------------------- listado

    @Test
    @DisplayName("mis simulacros salen paginados, el más reciente primero, y solo los míos")
    void misSimulacros() {
        iniciar(1);
        reloj.avanzar(Duration.ofMinutes(1));
        String segundo = iniciar(1).simulacro().getId();
        servicio.iniciar("estudiante2", Preguntas.criterios(1, 10));

        var pagina = servicio.misSimulacros("estudiante1", 1, 10);

        assertThat(pagina.totalElementos()).isEqualTo(2);
        assertThat(pagina.elementos().get(0).getId()).isEqualTo(segundo);
        assertThatThrownBy(() -> servicio.misSimulacros("estudiante1", 0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThat(servicio.misSimulacros("estudiante1", 2, 1).elementos()).hasSize(1);
    }

    @Test
    @DisplayName("las preguntas de un mismo simulacro no se repiten")
    void sinRepetidas() {
        List<String> ids = iniciar(7).simulacro().getItems().stream().map(i -> i.pregunta().preguntaId()).toList();

        assertThat(ids).doesNotHaveDuplicates();
    }
}
