package co.unicauca.bancopreguntas.review.application;

import co.unicauca.bancopreguntas.review.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.review.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.review.application.port.ReviewCaseRepository;
import co.unicauca.bancopreguntas.review.domain.Asignacion;
import co.unicauca.bancopreguntas.review.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.review.domain.CasoDeRevision;
import co.unicauca.bancopreguntas.review.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.review.domain.CasoPendiente;
import co.unicauca.bancopreguntas.review.domain.Decision;
import co.unicauca.bancopreguntas.review.domain.Evaluacion;
import co.unicauca.bancopreguntas.review.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.review.domain.Observador;
import co.unicauca.bancopreguntas.review.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.review.domain.Pagina;
import co.unicauca.bancopreguntas.review.domain.PoliticaDeDecision;
import co.unicauca.bancopreguntas.review.domain.ResultadoDeDecision;
import co.unicauca.bancopreguntas.review.domain.ResultadoDeEvaluacion;
import co.unicauca.bancopreguntas.review.domain.RondaDeRevision;
import co.unicauca.bancopreguntas.review.domain.RondaIniciada;
import co.unicauca.bancopreguntas.review.domain.VistaDelCaso;
import co.unicauca.bancopreguntas.review.domain.event.EvaluationSubmitted;
import co.unicauca.bancopreguntas.review.domain.event.ReviewCompleted;
import co.unicauca.bancopreguntas.review.domain.event.ReviewersAssigned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Casos de uso de la revisión por pares (HU-04 y HU-05) y reacción a los eventos del servicio de preguntas.
 * Coordina el dominio y los puertos; no conoce ni la base de datos ni el broker.
 */
@Transactional
public class ReviewApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewApplicationService.class);

    private final ReviewCaseRepository repositorio;
    private final DomainEventPublisher eventos;
    private final ProcessedEvents procesados;
    private final PoliticaDeDecision politica;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;
    private final int maximoDeRevisores;

    public ReviewApplicationService(ReviewCaseRepository repositorio, DomainEventPublisher eventos,
                                    ProcessedEvents procesados, PoliticaDeDecision politica, Clock reloj,
                                    Supplier<String> generadorDeId, int maximoDeRevisores) {
        if (maximoDeRevisores < 1) {
            throw new IllegalArgumentException("El máximo de revisores debe ser al menos 1");
        }
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.procesados = procesados;
        this.politica = politica;
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
        CasoDeRevision caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId)
                .orElseGet(() -> CasoDeRevision.nuevo(generadorDeId.get(), preguntaId, autorId, titulo, competencia, ahora));
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
            RondaDeRevision ronda = iniciada.ronda();
            eventos.publicar(new ReviewersAssigned(preguntaId, caso.autorId(), ronda.numero(), ronda.revisores(),
                    Asignacion.SISTEMA, true));
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
        CasoDeRevision caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId).orElse(null);
        if (caso == null) {
            LOG.warn("Llegó la reapertura de la pregunta {}, que no tiene revisión; se ignora", preguntaId);
            return;
        }
        if (caso.cerrarRondaPorReapertura(reloj.instant())) {
            repositorio.guardar(caso);
        }
    }

    // ------------------------------------------------------------------ operaciones del administrador y de los revisores

    /**
     * El administrador asigna revisores a la ronda vigente (HU-04), o agrega más a una ronda en curso. Publica
     * {@code review.reviewers-assigned} con los asignados en este acto.
     */
    public CasoDeRevision asignarRevisores(String preguntaId, List<String> revisores, String administradorId) {
        CasoDeRevision caso = cargarParaActualizar(preguntaId);
        List<Asignacion> nuevas = caso.asignarRevisores(revisores, administradorId, maximoDeRevisores, reloj.instant());
        repositorio.guardar(caso);
        eventos.publicar(new ReviewersAssigned(preguntaId, caso.autorId(), caso.rondaActual().numero(),
                nuevas.stream().map(Asignacion::revisorId).toList(), administradorId, false));
        return caso;
    }

    /**
     * Un revisor envía su evaluación (HU-05). Publica {@code review.evaluation-submitted} y, si con ella se
     * decide la ronda, {@code review.completed}.
     */
    public ResultadoDeEvaluacion evaluar(String preguntaId, String revisorId, Decision decision, String observacion) {
        CasoDeRevision caso = cargarParaActualizar(preguntaId);
        ResultadoDeEvaluacion resultado = caso.evaluar(revisorId, decision, observacion, politica, reloj.instant());
        repositorio.guardar(caso);
        Evaluacion evaluacion = resultado.evaluacion();
        int numeroDeRonda = resultado.ronda().numero();
        eventos.publicar(new EvaluationSubmitted(preguntaId, caso.autorId(), numeroDeRonda, revisorId,
                evaluacion.decision().name(), evaluacion.observacion(), evaluacion.tardia()));
        if (resultado.decidioLaRonda()) {
            String veredicto = resultado.decision() == ResultadoDeDecision.APROBADA
                    ? ReviewCompleted.APPROVED : ReviewCompleted.REJECTED;
            List<ReviewCompleted.Observation> observaciones = resultado.ronda().evaluaciones().stream()
                    .map(e -> new ReviewCompleted.Observation(e.revisorId(), e.decision().name(), e.observacion()))
                    .toList();
            eventos.publicar(new ReviewCompleted(preguntaId, caso.autorId(), numeroDeRonda, veredicto, observaciones));
        }
        return resultado;
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

    /**
     * La revisión de una pregunta tal como la ve el observador: el administrador y los revisores ven todo; el
     * autor, el detalle de las rondas ya decididas.
     */
    @Transactional(readOnly = true)
    public VistaDelCaso caso(String preguntaId, Observador observador) {
        CasoDeRevision caso = repositorio.buscarPorPreguntaId(preguntaId)
                .orElseThrow(() -> new CasoNoEncontradoException(preguntaId));
        return caso.vistaPara(observador);
    }

    // ------------------------------------------------------------------ auxiliares

    private CasoDeRevision cargarParaActualizar(String preguntaId) {
        return repositorio.buscarPorPreguntaIdParaActualizar(preguntaId)
                .orElseThrow(() -> new CasoNoEncontradoException(preguntaId));
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
