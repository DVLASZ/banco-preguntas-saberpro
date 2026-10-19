package co.unicauca.bancopreguntas.simulation.application;

import co.unicauca.bancopreguntas.simulation.application.port.CatalogRepository;
import co.unicauca.bancopreguntas.simulation.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.simulation.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.simulation.application.port.SimulacroRepository;
import co.unicauca.bancopreguntas.simulation.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.simulation.domain.CriteriosDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.CriteriosInvalidosException;
import co.unicauca.bancopreguntas.simulation.domain.Pagina;
import co.unicauca.bancopreguntas.simulation.domain.PreguntaDeCatalogo;
import co.unicauca.bancopreguntas.simulation.domain.PreguntasInsuficientesException;
import co.unicauca.bancopreguntas.simulation.domain.ResultadoDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.ResumenDelCatalogo;
import co.unicauca.bancopreguntas.simulation.domain.SelectorDePreguntas;
import co.unicauca.bancopreguntas.simulation.domain.Simulacro;
import co.unicauca.bancopreguntas.simulation.domain.SimulacroNoEncontradoException;
import co.unicauca.bancopreguntas.simulation.domain.TiempoAgotadoException;
import co.unicauca.bancopreguntas.simulation.domain.event.SimulationCompleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Casos de uso de los simulacros (RF-21 a RF-24): alimenta su catálogo con los eventos de preguntas publicadas,
 * genera simulacros aleatorios, controla el tiempo, califica automáticamente y avisa el resultado por un evento.
 * Los manejadores de eventos son idempotentes.
 */
@Transactional
public class SimulationApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(SimulationApplicationService.class);

    private final CatalogRepository catalogo;
    private final SimulacroRepository simulacros;
    private final DomainEventPublisher eventos;
    private final ProcessedEvents procesados;
    private final SelectorDePreguntas selector;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;
    private final int maximoDePreguntas;

    public SimulationApplicationService(CatalogRepository catalogo, SimulacroRepository simulacros,
                                        DomainEventPublisher eventos, ProcessedEvents procesados,
                                        SelectorDePreguntas selector, Clock reloj, Supplier<String> generadorDeId,
                                        int maximoDePreguntas) {
        this.catalogo = catalogo;
        this.simulacros = simulacros;
        this.eventos = eventos;
        this.procesados = procesados;
        this.selector = selector;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
        this.maximoDePreguntas = maximoDePreguntas;
    }

    // ------------------------------------------------------------------ reacción a eventos

    /** {@code question.published}: agrega la pregunta al catálogo (o actualiza su copia si la versión es más nueva). */
    public void alPublicarseUnaPregunta(String eventId, PreguntaDeCatalogo pregunta) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        PreguntaDeCatalogo actual = catalogo.buscar(pregunta.preguntaId()).orElse(null);
        if (actual != null && actual.version() > pregunta.version()) {
            LOG.info("Se ignora la versión {} de la pregunta {}: ya se conoce la {}", pregunta.version(),
                    pregunta.preguntaId(), actual.version());
            return;
        }
        catalogo.guardar(pregunta);
    }

    /** {@code question.state-changed}: una pregunta archivada sale del catálogo para los simulacros nuevos. */
    public void alCambiarElEstadoDeUnaPregunta(String eventId, String preguntaId, String hacia) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        if ("ARCHIVADA".equals(hacia)) {
            catalogo.buscar(preguntaId).ifPresent(p -> catalogo.guardar(p.retirada()));
        }
    }

    // ------------------------------------------------------------------ simulacros

    /**
     * Genera un simulacro aleatorio con preguntas publicadas (RF-21 y RF-22) y empieza a contar el tiempo (RF-23).
     *
     * @throws CriteriosInvalidosException      si pide más preguntas que el máximo permitido
     * @throws PreguntasInsuficientesException si no hay suficientes preguntas que cumplan los criterios
     */
    public VistaDeSimulacro iniciar(String estudianteId, CriteriosDeSimulacro criterios) {
        if (criterios.cantidad() > maximoDePreguntas) {
            throw new CriteriosInvalidosException("Un simulacro no puede tener más de " + maximoDePreguntas + " preguntas");
        }
        List<PreguntaDeCatalogo> candidatas = catalogo.disponibles().stream().filter(criterios::acepta).toList();
        if (candidatas.size() < criterios.cantidad()) {
            throw new PreguntasInsuficientesException(candidatas.size(), criterios.cantidad());
        }
        Instant ahora = reloj.instant();
        Simulacro simulacro = Simulacro.iniciar(generadorDeId.get(), estudianteId, criterios,
                selector.elegir(candidatas, criterios.cantidad()), ahora);
        simulacros.guardar(simulacro);
        return new VistaDeSimulacro(simulacro, simulacro.segundosRestantes(ahora));
    }

    /**
     * Consulta un simulacro. Si su tiempo ya se agotó, lo cierra y lo califica en ese momento.
     *
     * @param puedeVerTodos {@code true} para docentes y administradores, que consultan los de cualquier estudiante
     * @throws AccesoDenegadoException si es de otro estudiante y quien consulta no puede ver todos
     */
    public VistaDeSimulacro obtener(String id, String usuarioId, boolean puedeVerTodos) {
        Simulacro simulacro = buscar(id);
        verificarAcceso(simulacro, usuarioId, puedeVerTodos);
        Instant ahora = reloj.instant();
        cerrarSiVencio(simulacro, ahora);
        return new VistaDeSimulacro(simulacro, simulacro.segundosRestantes(ahora));
    }

    /**
     * Registra la respuesta de una pregunta.
     *
     * @throws TiempoAgotadoException si el tiempo ya terminó; el simulacro queda cerrado y calificado, por eso no se
     *                                deshace la transacción
     */
    @Transactional(noRollbackFor = TiempoAgotadoException.class)
    public VistaDeSimulacro responder(String id, String estudianteId, String preguntaId, char opcion) {
        Simulacro simulacro = buscar(id);
        verificarAcceso(simulacro, estudianteId, false);
        Instant ahora = reloj.instant();
        if (cerrarSiVencio(simulacro, ahora)) {
            throw new TiempoAgotadoException();
        }
        simulacro.responder(preguntaId, opcion, ahora);
        simulacros.guardar(simulacro);
        return new VistaDeSimulacro(simulacro, simulacro.segundosRestantes(ahora));
    }

    /** El estudiante termina el simulacro: se califica automáticamente (RF-24) y se publica el resultado. */
    public VistaDeSimulacro finalizar(String id, String estudianteId) {
        Simulacro simulacro = buscar(id);
        verificarAcceso(simulacro, estudianteId, false);
        Instant ahora = reloj.instant();
        simulacro.finalizar(ahora);
        cerrar(simulacro, ahora);
        return new VistaDeSimulacro(simulacro, 0);
    }

    @Transactional(readOnly = true)
    public Pagina<Simulacro> misSimulacros(String estudianteId, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1) {
            throw new IllegalArgumentException("La página y el tamaño deben ser al menos 1");
        }
        return simulacros.delEstudiante(estudianteId, pagina, tamano);
    }

    @Transactional(readOnly = true)
    public ResumenDelCatalogo resumenDelCatalogo() {
        return catalogo.resumen();
    }

    /** Cierra los simulacros cuyo tiempo se agotó aunque el estudiante no haya vuelto; devuelve cuántos cerró. */
    public int cerrarVencidos() {
        Instant ahora = reloj.instant();
        List<Simulacro> vencidos = simulacros.vencidos(ahora, 100);
        vencidos.forEach(s -> cerrarSiVencio(s, ahora));
        return vencidos.size();
    }

    // ------------------------------------------------------------------ auxiliares

    private Simulacro buscar(String id) {
        return simulacros.buscar(id).orElseThrow(() -> new SimulacroNoEncontradoException(id));
    }

    private static void verificarAcceso(Simulacro simulacro, String usuarioId, boolean puedeVerTodos) {
        if (!puedeVerTodos && !simulacro.getEstudianteId().equals(usuarioId)) {
            throw new AccesoDenegadoException("Este simulacro es de otro estudiante");
        }
    }

    /** Si el simulacro sigue en curso pero ya venció, lo califica y lo cierra. Devuelve si lo cerró. */
    private boolean cerrarSiVencio(Simulacro simulacro, Instant ahora) {
        if (simulacro.enCurso() && simulacro.vencido(ahora)) {
            simulacro.finalizar(ahora);
            cerrar(simulacro, ahora);
            return true;
        }
        return false;
    }

    /** Guarda el simulacro ya calificado y publica {@code simulation.completed}. */
    private void cerrar(Simulacro simulacro, Instant ahora) {
        simulacros.guardar(simulacro);
        ResultadoDeSimulacro r = simulacro.getResultado();
        eventos.publicar(new SimulationCompleted(simulacro.getId(), simulacro.getEstudianteId(),
                simulacro.getIniciadoEn(), simulacro.getFinalizadoEn(), simulacro.segundosUsados(ahora),
                simulacro.getEstado() == co.unicauca.bancopreguntas.simulation.domain.EstadoSimulacro.EXPIRADO,
                r.total(), r.correctas(), r.incorrectas(), r.sinResponder(), r.puntaje(),
                r.porCompetencia().stream().map(c -> new SimulationCompleted.ByCompetency(
                        c.competencia(), c.correctas(), c.total())).toList()));
    }
}
