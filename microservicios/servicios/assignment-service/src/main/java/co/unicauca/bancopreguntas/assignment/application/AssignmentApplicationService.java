package co.unicauca.bancopreguntas.assignment.application;

import co.unicauca.bancopreguntas.assignment.application.port.AssignmentRepository;
import co.unicauca.bancopreguntas.assignment.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.assignment.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.assignment.domain.Asignacion;
import co.unicauca.bancopreguntas.assignment.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.assignment.domain.CasoDeAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.assignment.domain.CasoPendiente;
import co.unicauca.bancopreguntas.assignment.domain.Decision;
import co.unicauca.bancopreguntas.assignment.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.assignment.domain.Pagina;
import co.unicauca.bancopreguntas.assignment.domain.RondaDeAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.RondaIniciada;
import co.unicauca.bancopreguntas.assignment.domain.event.ReviewersAssigned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Casos de uso de la asignación de revisores (HU-04) y de la bandeja del revisor (consulta de HU-05). Reacciona a los
 * eventos del servicio de preguntas (abrir y cerrar rondas) y de evaluation-service (recordar quién evaluó y cómo
 * terminó la ronda). Coordina el dominio y los puertos; no conoce ni la base de datos ni el broker.
 */
@Transactional
public class AssignmentApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(AssignmentApplicationService.class);

    private final AssignmentRepository repositorio;
    private final DomainEventPublisher eventos;
    private final ProcessedEvents procesados;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;
    private final int maximoDeRevisores;

    public AssignmentApplicationService(AssignmentRepository repositorio, DomainEventPublisher eventos,
                                        ProcessedEvents procesados, Clock reloj, Supplier<String> generadorDeId,
                                        int maximoDeRevisores) {
        if (maximoDeRevisores < 1) {
            throw new IllegalArgumentException("El máximo de revisores debe ser al menos 1");
        }
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.procesados = procesados;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
        this.maximoDeRevisores = maximoDeRevisores;
    }

    /** Cantidad máxima de revisores que admite una pregunta. */
    public int maximoDeRevisores() {
        return maximoDeRevisores;
    }

    // ------------------------------------------------------------------ reacción a eventos

    /**
     * Reacción a {@code question.submitted}: el autor envió una versión de la pregunta. Abre el caso si es la
     * primera vez y empieza una ronda. Si hay revisores de una ronda anterior, se reasignan solos y se publica
     * {@code review.reviewers-assigned}; si no, la ronda espera al administrador. Es idempotente.
     */
    public void alEnviarseUnaPregunta(String eventId, String preguntaId, String autorId, String titulo,
                                      String competencia, int version) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        Instant ahora = reloj.instant();
        CasoDeAsignacion caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId)
                .orElseGet(() -> CasoDeAsignacion.nuevo(generadorDeId.get(), preguntaId, autorId, titulo, competencia, ahora));
        RondaIniciada iniciada;
        try {
            iniciada = caso.iniciarRonda(version, ahora);
        } catch (OperacionNoPermitidaException e) {
            LOG.warn("Se ignora el envío a revisión de la pregunta {}: {}", preguntaId, e.getMessage());
            return;
        }
        if (!iniciada.nueva()) {
            return;
        }
        repositorio.guardar(caso);
        if (iniciada.conRevisoresReasignados()) {
            RondaDeAsignacion ronda = iniciada.ronda();
            eventos.publicar(reviewersAssigned(caso, ronda, ronda.revisores(), Asignacion.SISTEMA, true));
        }
    }

    /**
     * Reacción a {@code question.reopened}: el autor reabrió la pregunta rechazada para corregirla, así que la
     * ronda que seguía abierta a observaciones se cierra. Es idempotente.
     */
    public void alReabrirseUnaPregunta(String eventId, String preguntaId) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        CasoDeAsignacion caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId).orElse(null);
        if (caso == null) {
            LOG.warn("Llegó la reapertura de la pregunta {}, que no tiene revisión; se ignora", preguntaId);
            return;
        }
        if (caso.cerrarRondaPorReapertura(reloj.instant())) {
            repositorio.guardar(caso);
        }
    }

    /**
     * Reacción a {@code review.evaluation-submitted}: un revisor envió su evaluación. Se recuerda su decisión para que
     * la bandeja lo muestre como evaluada. Es idempotente.
     */
    public void alEvaluarseUnaPregunta(String eventId, String preguntaId, int ronda, String revisorId, Decision decision) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        CasoDeAsignacion caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId).orElse(null);
        if (caso == null) {
            LOG.warn("Llegó la evaluación de la pregunta {}, que no tiene asignaciones; se ignora", preguntaId);
            return;
        }
        if (caso.registrarEvaluacion(ronda, revisorId, decision, reloj.instant())) {
            repositorio.guardar(caso);
        }
    }

    /**
     * Reacción a {@code review.completed}: la ronda se aprobó o se rechazó, así que ya no admite más revisores. Es
     * idempotente.
     */
    public void alCompletarseLaRevision(String eventId, String preguntaId, int ronda, boolean aprobada) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        CasoDeAsignacion caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId).orElse(null);
        if (caso == null) {
            LOG.warn("Llegó el resultado de la pregunta {}, que no tiene asignaciones; se ignora", preguntaId);
            return;
        }
        if (caso.registrarDecisionDeRonda(ronda, aprobada, reloj.instant())) {
            repositorio.guardar(caso);
        }
    }

    // ------------------------------------------------------------------ operaciones del administrador

    /**
     * El administrador asigna revisores a la ronda vigente (HU-04), o agrega más a una ronda en curso. Publica
     * {@code review.reviewers-assigned} con los asignados en este acto.
     */
    public CasoDeAsignacion asignarRevisores(String preguntaId, List<String> revisores, String administradorId) {
        CasoDeAsignacion caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId)
                .orElseThrow(() -> new CasoNoEncontradoException(preguntaId));
        List<Asignacion> nuevas = caso.asignarRevisores(revisores, administradorId, maximoDeRevisores, reloj.instant());
        repositorio.guardar(caso);
        eventos.publicar(reviewersAssigned(caso, caso.rondaActual(),
                nuevas.stream().map(Asignacion::revisorId).toList(), administradorId, false));
        return caso;
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public Pagina<CasoPendiente> pendientesDeAsignacion(int pagina, int tamano) {
        validarPagina(pagina, tamano);
        return corregirPagina(pagina, tamano, repositorio.pendientesDeAsignacion(pagina, tamano),
                p -> repositorio.pendientesDeAsignacion(p, tamano));
    }

    @Transactional(readOnly = true)
    public Pagina<AsignacionDeRevisor> asignadasA(String revisorId, FiltroAsignacion filtro, int pagina, int tamano) {
        if (revisorId == null || revisorId.isBlank()) {
            throw new IllegalArgumentException("El revisor es obligatorio");
        }
        validarPagina(pagina, tamano);
        FiltroAsignacion criterio = filtro == null ? FiltroAsignacion.PENDIENTES : filtro;
        return corregirPagina(pagina, tamano, repositorio.asignadasA(revisorId, criterio, pagina, tamano),
                p -> repositorio.asignadasA(revisorId, criterio, p, tamano));
    }

    /** El caso de una pregunta con sus rondas y asignaciones. */
    @Transactional(readOnly = true)
    public CasoDeAsignacion caso(String preguntaId) {
        return repositorio.buscarPorPreguntaId(preguntaId).orElseThrow(() -> new CasoNoEncontradoException(preguntaId));
    }

    // ------------------------------------------------------------------ auxiliares

    private static ReviewersAssigned reviewersAssigned(CasoDeAsignacion caso, RondaDeAsignacion ronda,
                                                       List<String> revisores, String asignadoPor, boolean automatica) {
        return new ReviewersAssigned(caso.preguntaId(), caso.autorId(), ronda.numero(), revisores, asignadoPor,
                automatica, ronda.versionDePregunta(), caso.titulo(), caso.competencia());
    }

    private static void validarPagina(int pagina, int tamano) {
        if (tamano < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        }
        if (pagina < 1) {
            throw new IllegalArgumentException("La página debe ser al menos 1");
        }
    }

    /** Si se pide una página que no existe, devuelve la última. */
    private static <T> Pagina<T> corregirPagina(int pagina, int tamano, Pagina<T> resultado,
                                                java.util.function.IntFunction<Pagina<T>> buscar) {
        if (pagina > resultado.totalPaginas()) {
            return buscar.apply(resultado.totalPaginas());
        }
        return resultado;
    }
}
