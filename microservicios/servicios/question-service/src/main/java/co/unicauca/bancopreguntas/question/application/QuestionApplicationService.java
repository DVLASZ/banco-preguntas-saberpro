package co.unicauca.bancopreguntas.question.application;

import co.unicauca.bancopreguntas.question.application.port.CicloDeVidaDePreguntas;
import co.unicauca.bancopreguntas.question.application.port.EstadoDeCiclo;
import co.unicauca.bancopreguntas.question.application.port.QuestionRepository;
import co.unicauca.bancopreguntas.question.application.port.ValidadorDeContenido;
import co.unicauca.bancopreguntas.question.domain.AccesoDenegadoException;
import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.ContenidoPregunta;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;
import co.unicauca.bancopreguntas.question.domain.QuestionDistractors;
import co.unicauca.bancopreguntas.question.domain.QuestionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Casos de uso de la gestión de preguntas (HU-01, HU-02 y HU-03): guarda y consulta el contenido de las preguntas.
 * Para revisar la estructura del contenido usa validation-service y para mover una pregunta de estado usa
 * lifecycle-service, a través de puertos; no conoce ni la base de datos ni el otro servicio.
 *
 * <p>Ciclo de vida (RF-14 y RF-15): el autor crea siempre un {@code BORRADOR}, solo lo modifica mientras lo sea
 * (RF-06) y lo envía a revisión. Que pase a {@code EN_REVISION} y luego a {@code APROBADA} o {@code RECHAZADA} lo
 * deciden los eventos del servicio de revisión, que lifecycle-service aplica y avisa con {@code question.state-changed};
 * este servicio guarda una copia del estado para poder listar y filtrar.
 */
@Transactional
public class QuestionApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(QuestionApplicationService.class);

    private final QuestionRepository repositorio;
    private final ValidadorDeContenido validador;
    private final CicloDeVidaDePreguntas ciclo;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;

    public QuestionApplicationService(QuestionRepository repositorio, ValidadorDeContenido validador,
                                      CicloDeVidaDePreguntas ciclo, Clock reloj, Supplier<String> generadorDeId) {
        this.repositorio = repositorio;
        this.validador = validador;
        this.ciclo = ciclo;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
    }

    /**
     * Crea una pregunta a nombre de un autor, siempre en {@code BORRADOR} (HU-01). Antes de grabarla aplica la
     * validación estructural y la registra en el ciclo de vida: si algo falla, no se guarda nada.
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
        EstadoDeCiclo registrada = ciclo.registrar(id);
        Question pregunta = construir(id, contenido, registrada.estado(), autor.trim(), ahora, ahora,
                registrada.version(), registrada.secuencia());
        return repositorio.guardar(pregunta);
    }

    @Transactional(readOnly = true)
    public Question obtener(String id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> new QuestionNotFoundException(id));
    }

    /** El historial de estados de la pregunta, que lleva lifecycle-service. */
    @Transactional(readOnly = true)
    public List<CambioEstado> historial(String id) {
        obtener(id);
        return ciclo.historial(id);
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
        actual.exigirEditable();
        validador.validarOLanzar(contenido);
        Question modificada = construir(id, contenido, actual.getEstado(), actual.getAutor(),
                actual.getCreadaEn(), reloj.instant(), actual.getVersion(), actual.getEstadoSecuencia());
        return repositorio.guardar(modificada);
    }

    /**
     * El autor envía su borrador a revisión (HU-02): se vuelve a aplicar la validación estructural y, si la pasa,
     * lifecycle-service la deja en {@code PENDIENTE_REVISION} y publica {@code question.submitted}.
     */
    public Question enviarARevision(String id, String usuario) {
        Question actual = obtener(id);
        exigirAutor(actual, usuario);
        actual.exigirEditable();
        validador.validarOLanzar(ContenidoPregunta.de(actual));
        return aplicar(actual, ciclo.enviarARevision(actual));
    }

    /**
     * El autor reabre su pregunta rechazada para corregirla: vuelve a {@code BORRADOR} como una versión nueva y
     * lifecycle-service publica {@code question.reopened}, con el que el servicio de revisión cierra la ronda que
     * seguía abierta a observaciones.
     */
    public Question reabrirParaCorregir(String id, String usuario) {
        Question actual = obtener(id);
        exigirAutor(actual, usuario);
        return aplicar(actual, ciclo.reabrir(id));
    }

    /**
     * El administrador publica una pregunta aprobada (de {@code APROBADA} a {@code PUBLICADA}); lifecycle-service
     * publica {@code question.published} con el contenido de la pregunta.
     */
    public Question publicar(String id) {
        Question actual = obtener(id);
        return aplicar(actual, ciclo.publicar(actual));
    }

    /**
     * Reacción a {@code question.state-changed}: lifecycle-service cambió el estado de la pregunta (por ejemplo, la
     * revisión la aprobó) y esta copia se actualiza. Un aviso repetido o atrasado, con una secuencia que ya se aplicó,
     * se ignora; por eso no necesita llevar registro de eventos procesados.
     */
    public void alCambiarElEstado(String preguntaId, EstadoPregunta hacia, int version, int secuencia) {
        Question pregunta = repositorio.buscarPorId(preguntaId).orElse(null);
        if (pregunta == null) {
            LOG.warn("Llegó un cambio de estado de la pregunta {}, que no existe; se ignora", preguntaId);
            return;
        }
        Question actualizada = pregunta.conEstado(hacia, version, secuencia, reloj.instant());
        if (actualizada != pregunta) {
            repositorio.guardar(actualizada);
        }
    }

    /** Guarda en la copia el estado que informó lifecycle-service y devuelve la pregunta actualizada. */
    private Question aplicar(Question pregunta, EstadoDeCiclo nuevo) {
        return repositorio.guardar(pregunta.conEstado(nuevo.estado(), nuevo.version(), nuevo.secuencia(), reloj.instant()));
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
                                      Instant creadaEn, Instant actualizadaEn, int version, int secuencia) {
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
                .estadoSecuencia(secuencia)
                .build();
    }
}
