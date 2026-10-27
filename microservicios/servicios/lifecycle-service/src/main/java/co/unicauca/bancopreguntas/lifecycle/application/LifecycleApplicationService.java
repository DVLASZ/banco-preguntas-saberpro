package co.unicauca.bancopreguntas.lifecycle.application;

import co.unicauca.bancopreguntas.lifecycle.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.lifecycle.application.port.LifecycleRepository;
import co.unicauca.bancopreguntas.lifecycle.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.lifecycle.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.lifecycle.domain.CambioEstado;
import co.unicauca.bancopreguntas.lifecycle.domain.CicloDeVida;
import co.unicauca.bancopreguntas.lifecycle.domain.ContenidoPublicado;
import co.unicauca.bancopreguntas.lifecycle.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.lifecycle.domain.PreguntaNoRegistradaException;
import co.unicauca.bancopreguntas.lifecycle.domain.event.QuestionPublished;
import co.unicauca.bancopreguntas.lifecycle.domain.event.QuestionReopened;
import co.unicauca.bancopreguntas.lifecycle.domain.event.QuestionStateChanged;
import co.unicauca.bancopreguntas.lifecycle.domain.event.QuestionSubmitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Casos de uso del ciclo de vida de las preguntas (RF-14 y RF-15): registra cada pregunta nueva en Borrador, aplica
 * las transiciones que piden el autor y el administrador, reacciona a los eventos del servicio de revisión y avisa
 * cada cambio por eventos. Coordina el dominio y los puertos; no conoce ni la base de datos ni el broker.
 *
 * <p>Que una pregunta pase a {@code EN_REVISION} y luego a {@code APROBADA} o {@code RECHAZADA} lo deciden los eventos
 * de revisión, no una llamada directa. Los manejadores de eventos son idempotentes.
 */
@Transactional
public class LifecycleApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(LifecycleApplicationService.class);

    private final LifecycleRepository repositorio;
    private final DomainEventPublisher eventos;
    private final ProcessedEvents procesados;
    private final Clock reloj;

    public LifecycleApplicationService(LifecycleRepository repositorio, DomainEventPublisher eventos,
                                       ProcessedEvents procesados, Clock reloj) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.procesados = procesados;
        this.reloj = reloj;
    }

    /**
     * Registra una pregunta recién creada en {@code BORRADOR} (HU-01). Es idempotente para el mismo autor.
     *
     * @throws OperacionNoPermitidaException si la pregunta ya está registrada a nombre de otro autor
     */
    public CicloDeVida registrar(String preguntaId, String autor) {
        CicloDeVida existente = repositorio.buscar(preguntaId).orElse(null);
        if (existente != null) {
            if (!existente.getAutorId().equals(autor == null ? null : autor.trim())) {
                throw new OperacionNoPermitidaException("La pregunta " + preguntaId + " ya está registrada");
            }
            return existente;
        }
        Instant ahora = reloj.instant();
        CicloDeVida ciclo = CicloDeVida.registrar(preguntaId, autor, ahora);
        repositorio.guardar(ciclo);
        repositorio.registrarCambioEstado(preguntaId, new CambioEstado(null, ciclo.getEstado(), ciclo.getAutorId(), ahora));
        return ciclo;
    }

    @Transactional(readOnly = true)
    public CicloDeVida obtener(String preguntaId) {
        return repositorio.buscar(preguntaId).orElseThrow(() -> new PreguntaNoRegistradaException(preguntaId));
    }

    @Transactional(readOnly = true)
    public List<CambioEstado> historial(String preguntaId) {
        obtener(preguntaId);
        return repositorio.historial(preguntaId);
    }

    /**
     * El autor envía su borrador a revisión (HU-02): queda en {@code PENDIENTE_REVISION} y se publican
     * {@code question.state-changed} y {@code question.submitted}. El contenido ya lo validó el servicio de preguntas.
     */
    public CicloDeVida enviarARevision(String preguntaId, String usuario, String titulo, String competencia) {
        CicloDeVida actual = obtener(preguntaId);
        exigirAutor(actual, usuario);
        CicloDeVida enviada = cambiar(actual, actual.enviarARevision(reloj.instant()), usuario.trim());
        eventos.publicar(new QuestionSubmitted(preguntaId, enviada.getAutorId(), titulo, competencia, enviada.getVersion()));
        return enviada;
    }

    /**
     * El autor reabre su pregunta rechazada para corregirla: vuelve a {@code BORRADOR} como una versión nueva y se
     * publican {@code question.state-changed} y {@code question.reopened}, con el que el servicio de revisión cierra la
     * ronda que seguía abierta a observaciones.
     */
    public CicloDeVida reabrir(String preguntaId, String usuario) {
        CicloDeVida actual = obtener(preguntaId);
        exigirAutor(actual, usuario);
        CicloDeVida reabierta = cambiar(actual, actual.reabrir(reloj.instant()), usuario.trim());
        eventos.publicar(new QuestionReopened(preguntaId, reabierta.getAutorId(), reabierta.getVersion()));
        return reabierta;
    }

    /**
     * El administrador publica una pregunta aprobada (de {@code APROBADA} a {@code PUBLICADA}) y se publican
     * {@code question.state-changed} y {@code question.published}, este último con el contenido recibido.
     */
    public CicloDeVida publicar(String preguntaId, String administrador, ContenidoPublicado contenido) {
        CicloDeVida actual = obtener(preguntaId);
        CicloDeVida publicada = cambiar(actual, actual.publicar(reloj.instant()), administrador.trim());
        eventos.publicar(QuestionPublished.de(preguntaId, publicada.getVersion(), administrador.trim(), contenido));
        return publicada;
    }

    /**
     * Reacción a {@code review.reviewers-assigned}: el administrador asignó revisores y la pregunta pasa a
     * {@code EN_REVISION}. Es idempotente: un evento repetido se ignora.
     */
    public void aplicarRevisoresAsignados(String eventId, String preguntaId) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        aplicarCambioPorEvento(preguntaId, c -> c.iniciarRevision(reloj.instant()));
    }

    /**
     * Reacción a {@code review.completed}: la revisión terminó y la pregunta pasa de {@code EN_REVISION} a
     * {@code APROBADA} o {@code RECHAZADA}. Es idempotente.
     */
    public void aplicarRevisionCompletada(String eventId, String preguntaId, boolean aprobada) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        aplicarCambioPorEvento(preguntaId, c -> aprobada ? c.aprobar(reloj.instant()) : c.rechazar(reloj.instant()));
    }

    /** Aplica el cambio que pide un evento; si la fase actual de la pregunta no lo admite, el evento se ignora. */
    private void aplicarCambioPorEvento(String preguntaId, UnaryOperator<CicloDeVida> transicion) {
        CicloDeVida actual = repositorio.buscar(preguntaId).orElse(null);
        if (actual == null) {
            LOG.warn("Llegó un evento para la pregunta {}, que no existe; se ignora", preguntaId);
            return;
        }
        CicloDeVida nuevo;
        try {
            nuevo = transicion.apply(actual);
        } catch (OperacionNoPermitidaException e) {
            LOG.warn("La pregunta {} está en {} y no admite el cambio que pide el evento; se ignora",
                    preguntaId, actual.getEstado());
            return;
        }
        cambiar(actual, nuevo, null);
    }

    /** Guarda el nuevo ciclo, lo anota en el historial y publica {@code question.state-changed}. */
    private CicloDeVida cambiar(CicloDeVida anterior, CicloDeVida nuevo, String usuario) {
        repositorio.guardar(nuevo);
        repositorio.registrarCambioEstado(nuevo.getPreguntaId(),
                new CambioEstado(anterior.getEstado(), nuevo.getEstado(), usuario, nuevo.getActualizadoEn()));
        eventos.publicar(new QuestionStateChanged(nuevo.getPreguntaId(), anterior.getEstado().name(),
                nuevo.getEstado().name(), usuario, nuevo.getSecuencia(), nuevo.getVersion()));
        return nuevo;
    }

    private static void exigirAutor(CicloDeVida ciclo, String usuario) {
        if (usuario == null || !ciclo.getAutorId().equals(usuario.trim())) {
            throw new AccesoDenegadoException("Solo el autor de la pregunta puede realizar esta operación");
        }
    }
}
