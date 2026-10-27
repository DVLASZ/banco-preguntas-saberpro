package co.unicauca.bancopreguntas.evaluation.application;

import co.unicauca.bancopreguntas.evaluation.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.evaluation.application.port.EvaluationRepository;
import co.unicauca.bancopreguntas.evaluation.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.evaluation.domain.CasoDeEvaluacion;
import co.unicauca.bancopreguntas.evaluation.domain.CasoNoEncontradoException;
import co.unicauca.bancopreguntas.evaluation.domain.Decision;
import co.unicauca.bancopreguntas.evaluation.domain.Evaluacion;
import co.unicauca.bancopreguntas.evaluation.domain.Observador;
import co.unicauca.bancopreguntas.evaluation.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.evaluation.domain.PoliticaDeDecision;
import co.unicauca.bancopreguntas.evaluation.domain.ResultadoDeDecision;
import co.unicauca.bancopreguntas.evaluation.domain.ResultadoDeEvaluacion;
import co.unicauca.bancopreguntas.evaluation.domain.VistaDelCaso;
import co.unicauca.bancopreguntas.evaluation.domain.event.EvaluationSubmitted;
import co.unicauca.bancopreguntas.evaluation.domain.event.ReviewCompleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Casos de uso de la evaluación de preguntas por pares (HU-05) y reacción a los eventos del servicio de preguntas y de
 * assignment-service. Coordina el dominio y los puertos; no conoce ni la base de datos ni el broker.
 */
@Transactional
public class EvaluationApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationApplicationService.class);

    private final EvaluationRepository repositorio;
    private final DomainEventPublisher eventos;
    private final ProcessedEvents procesados;
    private final PoliticaDeDecision politica;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;

    public EvaluationApplicationService(EvaluationRepository repositorio, DomainEventPublisher eventos,
                                        ProcessedEvents procesados, PoliticaDeDecision politica, Clock reloj,
                                        Supplier<String> generadorDeId) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.procesados = procesados;
        this.politica = politica;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
    }

    // ------------------------------------------------------------------ reacción a eventos

    /**
     * Reacción a {@code question.submitted}: el autor envió una versión de la pregunta. Abre el caso si es la primera
     * vez y empieza una ronda, que espera a los revisores que asigne assignment-service. Es idempotente.
     */
    public void alEnviarseUnaPregunta(String eventId, String preguntaId, String autorId, String titulo,
                                      String competencia, int version) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        abrirRonda(preguntaId, autorId, titulo, competencia, version);
    }

    /**
     * Reacción a {@code review.reviewers-assigned}: assignment-service asignó revisores a una ronda. Se registran para
     * que puedan evaluar. Si el aviso llega antes que el del envío de la pregunta, la ronda se abre con los datos que
     * trae el propio evento. Es idempotente.
     */
    public void alAsignarseRevisores(String eventId, String preguntaId, String autorId, String titulo, String competencia,
                                     int version, int ronda, List<String> revisores, String asignadoPor,
                                     boolean automatica, Instant ocurridoEn) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        CasoDeEvaluacion caso = abrirRonda(preguntaId, autorId, titulo, competencia, version);
        if (caso == null) {
            return;
        }
        try {
            if (caso.registrarAsignaciones(ronda, revisores, asignadoPor, automatica,
                    ocurridoEn == null ? reloj.instant() : ocurridoEn) > 0) {
                repositorio.guardar(caso);
            }
        } catch (OperacionNoPermitidaException e) {
            LOG.warn("Se ignora la asignación de revisores de la pregunta {}: {}", preguntaId, e.getMessage());
        }
    }

    /**
     * Reacción a {@code question.reopened}: el autor reabrió la pregunta rechazada para corregirla, así que la ronda que
     * seguía abierta a observaciones se cierra. Es idempotente.
     */
    public void alReabrirseUnaPregunta(String eventId, String preguntaId) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        CasoDeEvaluacion caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId).orElse(null);
        if (caso == null) {
            LOG.warn("Llegó la reapertura de la pregunta {}, que no tiene revisión; se ignora", preguntaId);
            return;
        }
        if (caso.cerrarRondaPorReapertura(reloj.instant())) {
            repositorio.guardar(caso);
        }
    }

    /** Abre el caso y la ronda de esa versión si hacen falta; devuelve el caso, o {@code null} si no se puede abrir. */
    private CasoDeEvaluacion abrirRonda(String preguntaId, String autorId, String titulo, String competencia, int version) {
        Instant ahora = reloj.instant();
        CasoDeEvaluacion caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId)
                .orElseGet(() -> CasoDeEvaluacion.nuevo(generadorDeId.get(), preguntaId, autorId, titulo, competencia, ahora));
        try {
            if (caso.iniciarRonda(version, ahora).nueva()) {
                repositorio.guardar(caso);
            }
        } catch (OperacionNoPermitidaException e) {
            LOG.warn("Se ignora el envío a revisión de la pregunta {}: {}", preguntaId, e.getMessage());
            return null;
        }
        return caso;
    }

    // ------------------------------------------------------------------ operaciones del revisor

    /**
     * Un revisor envía su evaluación (HU-05). Publica {@code review.evaluation-submitted} y, si con ella se decide la
     * ronda, {@code review.completed}.
     */
    public ResultadoDeEvaluacion evaluar(String preguntaId, String revisorId, Decision decision, String observacion) {
        CasoDeEvaluacion caso = repositorio.buscarPorPreguntaIdParaActualizar(preguntaId)
                .orElseThrow(() -> new CasoNoEncontradoException(preguntaId));
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

    /**
     * La revisión de una pregunta tal como la ve el observador: el administrador y los revisores ven todo; el autor, el
     * detalle de las rondas ya decididas.
     */
    @Transactional(readOnly = true)
    public VistaDelCaso caso(String preguntaId, Observador observador) {
        CasoDeEvaluacion caso = repositorio.buscarPorPreguntaId(preguntaId)
                .orElseThrow(() -> new CasoNoEncontradoException(preguntaId));
        return caso.vistaPara(observador);
    }
}
