package co.unicauca.bancopreguntas.question.application;

import co.unicauca.bancopreguntas.question.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.question.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.question.application.port.QuestionRepository;
import co.unicauca.bancopreguntas.question.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.ContenidoPregunta;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;
import co.unicauca.bancopreguntas.question.domain.QuestionDistractors;
import co.unicauca.bancopreguntas.question.domain.QuestionNotFoundException;
import co.unicauca.bancopreguntas.question.domain.event.QuestionPublished;
import co.unicauca.bancopreguntas.question.domain.event.QuestionReopened;
import co.unicauca.bancopreguntas.question.domain.event.QuestionStateChanged;
import co.unicauca.bancopreguntas.question.domain.event.QuestionSubmitted;
import co.unicauca.bancopreguntas.question.domain.validation.QuestionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Casos de uso de la gestión de preguntas (HU-01, HU-02 y HU-03) y reacción a los eventos del
 * servicio de revisión. Coordina el dominio y los puertos; no conoce ni la base de datos ni el broker.
 *
 * <p>Ciclo de vida (RF-14 y RF-15): el autor crea siempre un {@code BORRADOR}, solo lo modifica mientras
 * lo sea (RF-06) y lo envía a revisión. Que pase a {@code EN_REVISION} y luego a {@code APROBADA} o
 * {@code RECHAZADA} lo deciden los eventos del servicio de revisión, no una llamada directa.
 */
@Transactional
public class QuestionApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(QuestionApplicationService.class);

    private final QuestionRepository repositorio;
    private final DomainEventPublisher eventos;
    private final ProcessedEvents procesados;
    private final QuestionValidator validador;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;

    public QuestionApplicationService(QuestionRepository repositorio, DomainEventPublisher eventos,
                                      ProcessedEvents procesados, QuestionValidator validador,
                                      Clock reloj, Supplier<String> generadorDeId) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.procesados = procesados;
        this.validador = validador;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
    }

    /**
     * Crea una pregunta a nombre de un autor, siempre en {@code BORRADOR} (HU-01). Antes de grabarla
     * aplica la validación estructural: si falla, no se guarda nada.
     *
     * @throws co.unicauca.bancopreguntas.question.domain.validation.QuestionValidationException
     *         con todos los campos que incumplen
     */
    public Question crearBorrador(ContenidoPregunta contenido, String autor) {
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException("El autor de la pregunta es obligatorio");
        }
        validador.validarOLanzar(contenido);
        Instant ahora = reloj.instant();
        String id = generadorDeId.get();
        Question pregunta = construir(id, contenido, EstadoPregunta.BORRADOR, autor.trim(), ahora, ahora, 1);
        Question guardada = repositorio.guardar(pregunta);
        repositorio.registrarCambioEstado(id, new CambioEstado(null, EstadoPregunta.BORRADOR, autor.trim(), ahora));
        return guardada;
    }

    @Transactional(readOnly = true)
    public Question obtener(String id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> new QuestionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<CambioEstado> historial(String id) {
        obtener(id);
        return repositorio.historial(id);
    }

    /**
     * Las preguntas de un autor que cumplen el filtro, de a una página (HU-03, RF-07). Si se pide una
     * página que no existe se devuelve la última; si aún no hay resultados, la primera vacía.
     *
     * @param pagina número de página, desde 1
     * @param tamano preguntas por página, al menos 1
     */
    @Transactional(readOnly = true)
    public Pagina<Question> buscarDelAutor(String autor, FiltroPreguntas filtro, int pagina, int tamano) {
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException("El autor es obligatorio");
        }
        return buscar(autor.trim(), filtro, pagina, tamano);
    }

    /** Las preguntas que esperan que el administrador les asigne revisores (HU-04). */
    @Transactional(readOnly = true)
    public Pagina<Question> buscarPendientesDeRevision(int pagina, int tamano) {
        return buscar(null, new FiltroPreguntas(EstadoPregunta.PENDIENTE_REVISION, null, null), pagina, tamano);
    }

    /**
     * Búsqueda del administrador sobre las preguntas de todos los autores (por ejemplo, las aprobadas que
     * esperan publicación). Un autor en blanco no filtra por autor.
     */
    @Transactional(readOnly = true)
    public Pagina<Question> buscarTodas(String autor, FiltroPreguntas filtro, int pagina, int tamano) {
        String autorFiltrado = autor == null || autor.isBlank() ? null : autor.trim();
        return buscar(autorFiltrado, filtro, pagina, tamano);
    }

    /**
     * Modifica el contenido de una pregunta sin tocar su estado. Solo lo puede hacer su autor y solo
     * mientras esté en {@code BORRADOR} (RF-06). Aplica la misma validación estructural que al crearla.
     */
    public Question actualizarContenido(String id, ContenidoPregunta contenido, String usuario) {
        Question actual = obtener(id);
        exigirAutor(actual, usuario);
        if (actual.getEstado() != EstadoPregunta.BORRADOR) {
            throw new OperacionNoPermitidaException(
                    "Solo se puede modificar una pregunta en estado Borrador; esta está " + actual.getEstado());
        }
        validador.validarOLanzar(contenido);
        Question modificada = construir(id, contenido, actual.getEstado(), actual.getAutor(),
                actual.getCreadaEn(), reloj.instant(), actual.getVersion());
        return repositorio.guardar(modificada);
    }

    /**
     * El autor envía su borrador a revisión (HU-02): se vuelve a aplicar la validación estructural y,
     * si la pasa, la pregunta queda en {@code PENDIENTE_REVISION} y se publica {@code question.submitted}.
     */
    public Question enviarARevision(String id, String usuario) {
        Question actual = obtener(id);
        exigirAutor(actual, usuario);
        if (actual.getEstado() != EstadoPregunta.BORRADOR) {
            throw new OperacionNoPermitidaException(
                    "Solo se puede enviar a revisión una pregunta en estado Borrador; esta está " + actual.getEstado());
        }
        validador.validarOLanzar(ContenidoPregunta.de(actual));
        Question enviada = cambiarEstado(actual, EstadoPregunta.PENDIENTE_REVISION, usuario.trim());
        eventos.publicar(new QuestionSubmitted(id, enviada.getAutor(), enviada.getNombre(),
                enviada.getCompetencia().name(), enviada.getVersion()));
        return enviada;
    }

    /**
     * El autor reabre su pregunta rechazada para corregirla: vuelve a {@code BORRADOR} como una versión
     * nueva y se publica {@code question.reopened}, con el que el servicio de revisión cierra la ronda que
     * seguía abierta a observaciones.
     */
    public Question reabrirParaCorregir(String id, String usuario) {
        Question actual = obtener(id);
        exigirAutor(actual, usuario);
        Instant ahora = reloj.instant();
        Question reabierta = repositorio.guardar(actual.reabrir(ahora));
        repositorio.registrarCambioEstado(id,
                new CambioEstado(actual.getEstado(), reabierta.getEstado(), usuario.trim(), ahora));
        eventos.publicar(new QuestionStateChanged(id, actual.getEstado().name(),
                reabierta.getEstado().name(), usuario.trim()));
        eventos.publicar(new QuestionReopened(id, reabierta.getAutor(), reabierta.getVersion()));
        return reabierta;
    }

    /**
     * El administrador publica una pregunta aprobada (de {@code APROBADA} a {@code PUBLICADA}) y se
     * publica {@code question.published}.
     */
    public Question publicar(String id, String administrador) {
        Question actual = obtener(id);
        if (actual.getEstado() != EstadoPregunta.APROBADA) {
            throw new OperacionNoPermitidaException(
                    "Solo se puede publicar una pregunta Aprobada; esta está " + actual.getEstado());
        }
        Question publicada = cambiarEstado(actual, EstadoPregunta.PUBLICADA, administrador.trim());
        eventos.publicar(new QuestionPublished(id, administrador.trim()));
        return publicada;
    }

    /**
     * Reacción a {@code review.reviewers-assigned}: el administrador asignó revisores y la pregunta
     * pasa a {@code EN_REVISION}. Es idempotente: un evento repetido se ignora.
     */
    public void aplicarRevisoresAsignados(String eventId, String preguntaId) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        aplicarCambioPorEvento(preguntaId, EstadoPregunta.PENDIENTE_REVISION, EstadoPregunta.EN_REVISION);
    }

    /**
     * Reacción a {@code review.completed}: la revisión terminó y la pregunta pasa de {@code EN_REVISION}
     * a {@code APROBADA} o {@code RECHAZADA}. Es idempotente.
     */
    public void aplicarRevisionCompletada(String eventId, String preguntaId, boolean aprobada) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            LOG.debug("Evento {} ya procesado; se ignora", eventId);
            return;
        }
        aplicarCambioPorEvento(preguntaId, EstadoPregunta.EN_REVISION,
                aprobada ? EstadoPregunta.APROBADA : EstadoPregunta.RECHAZADA);
    }

    private void aplicarCambioPorEvento(String preguntaId, EstadoPregunta esperado, EstadoPregunta nuevo) {
        Question pregunta = repositorio.buscarPorId(preguntaId).orElse(null);
        if (pregunta == null) {
            LOG.warn("Llegó un evento para la pregunta {}, que no existe; se ignora", preguntaId);
            return;
        }
        if (pregunta.getEstado() != esperado) {
            LOG.warn("La pregunta {} está en {} y el evento esperaba {}; se ignora",
                    preguntaId, pregunta.getEstado(), esperado);
            return;
        }
        cambiarEstado(pregunta, nuevo, null);
    }

    private Question cambiarEstado(Question pregunta, EstadoPregunta nuevo, String usuario) {
        Instant ahora = reloj.instant();
        EstadoPregunta anterior = pregunta.getEstado();
        Question actualizada = repositorio.guardar(pregunta.conEstado(nuevo, ahora));
        repositorio.registrarCambioEstado(pregunta.getId(), new CambioEstado(anterior, nuevo, usuario, ahora));
        eventos.publicar(new QuestionStateChanged(pregunta.getId(), anterior.name(), nuevo.name(), usuario));
        return actualizada;
    }

    private Pagina<Question> buscar(String autor, FiltroPreguntas filtro, int pagina, int tamano) {
        if (tamano < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        }
        if (pagina < 1) {
            throw new IllegalArgumentException("La página debe ser al menos 1");
        }
        FiltroPreguntas criterios = filtro == null ? FiltroPreguntas.sinFiltros() : filtro;
        Pagina<Question> resultado = repositorio.buscar(autor, criterios, pagina, tamano);
        if (pagina > resultado.totalPaginas()) {
            return repositorio.buscar(autor, criterios, resultado.totalPaginas(), tamano);
        }
        return resultado;
    }

    private static void exigirAutor(Question pregunta, String usuario) {
        if (usuario == null || !pregunta.getAutor().equals(usuario.trim())) {
            throw new AccesoDenegadoException("Solo el autor de la pregunta puede realizar esta operación");
        }
    }

    private static Question construir(String id, ContenidoPregunta c, EstadoPregunta estado, String autor,
                                      Instant creadaEn, Instant actualizadaEn, int version) {
        return Question.builder()
                .id(id)
                .nombre(c.nombre().trim())
                .contexto(c.contexto().trim())
                .enunciado(c.enunciado().trim())
                .opciones(new QuestionDistractors(c.opcionA().trim(), c.opcionB().trim(),
                        c.opcionC().trim(), c.opcionD().trim()))
                .respuestaCorrecta(c.respuestaCorrecta().trim().charAt(0))
                .justificacion(c.justificacion().trim())
                .bibliografia(c.bibliografia().trim())
                .estado(estado)
                .competencia(c.competencia())
                .tema(c.tema().trim())
                .subtema(c.subtema().trim())
                .dificultad(c.dificultad())
                .autor(autor)
                .creadaEn(creadaEn)
                .actualizadaEn(actualizadaEn)
                .version(version)
                .build();
    }
}
