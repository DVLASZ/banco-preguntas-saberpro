package co.unicauca.bancopreguntas.history.application;

import co.unicauca.bancopreguntas.history.application.port.HistoryRepository;
import co.unicauca.bancopreguntas.history.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.history.application.port.QuestionRegistry;
import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.EventoRecibido;
import co.unicauca.bancopreguntas.history.domain.HistorialNoEncontradoException;
import co.unicauca.bancopreguntas.history.domain.InfoPregunta;
import co.unicauca.bancopreguntas.history.domain.Observador;
import co.unicauca.bancopreguntas.history.domain.Pagina;
import co.unicauca.bancopreguntas.history.domain.Resumenes;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Casos de uso del historial: registra cada hecho que anuncian los demás servicios y lo consulta según quién
 * pregunta. Los manejadores de eventos son idempotentes (un evento repetido no deja una segunda entrada).
 */
@Transactional
public class HistoryApplicationService {

    /** Lo que el autor ve del rastro de su pregunta. */
    private static final Set<TipoEntrada> VISIBLES_PARA_EL_AUTOR = Arrays.stream(TipoEntrada.values())
            .filter(TipoEntrada::visibleParaElAutor)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(TipoEntrada.class)));

    private final HistoryRepository historial;
    private final QuestionRegistry preguntas;
    private final ProcessedEvents procesados;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;

    public HistoryApplicationService(HistoryRepository historial, QuestionRegistry preguntas,
                                     ProcessedEvents procesados, Clock reloj, Supplier<String> generadorDeId) {
        this.historial = historial;
        this.preguntas = preguntas;
        this.procesados = procesados;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
    }

    /** Los tipos de entrada que ve el autor de una pregunta. */
    public static Set<TipoEntrada> tiposVisiblesParaElAutor() {
        return Set.copyOf(VISIBLES_PARA_EL_AUTOR);
    }

    // ------------------------------------------------------------------ registro de eventos

    /** {@code question.submitted}: aprende el título y el autor de la pregunta y registra el envío. */
    public void alEnviarseUnaPregunta(EventoRecibido evento, String preguntaId, String autorId, String titulo, int version) {
        registrar(evento, TipoEntrada.PREGUNTA_ENVIADA, preguntaId, autorId,
                Resumenes.preguntaEnviada(autorId, titulo, version),
                () -> preguntas.guardar(new InfoPregunta(preguntaId, titulo, autorId)));
    }

    public void alReabrirseUnaPregunta(EventoRecibido evento, String preguntaId, String autorId, int version) {
        registrar(evento, TipoEntrada.PREGUNTA_REABIERTA, preguntaId, autorId,
                Resumenes.preguntaReabierta(autorId, version), null);
    }

    public void alPublicarseUnaPregunta(EventoRecibido evento, String preguntaId, String publicadaPor) {
        registrar(evento, TipoEntrada.PREGUNTA_PUBLICADA, preguntaId, publicadaPor,
                Resumenes.preguntaPublicada(publicadaPor), null);
    }

    public void alCambiarDeEstado(EventoRecibido evento, String preguntaId, String desde, String hacia, String cambiadoPor) {
        registrar(evento, TipoEntrada.CAMBIO_DE_ESTADO, preguntaId, cambiadoPor,
                Resumenes.cambioDeEstado(desde, hacia, cambiadoPor), null);
    }

    /** {@code review.reviewers-assigned}: registra la asignación y recuerda a los revisores de la pregunta. */
    public void alAsignarseRevisores(EventoRecibido evento, String preguntaId, int ronda, List<String> revisores,
                                     String asignadoPor, boolean automatica) {
        registrar(evento, TipoEntrada.REVISORES_ASIGNADOS, preguntaId, asignadoPor,
                Resumenes.revisoresAsignados(asignadoPor, ronda, revisores, automatica),
                () -> preguntas.agregarRevisores(preguntaId, revisores));
    }

    public void alEnviarseUnaEvaluacion(EventoRecibido evento, String preguntaId, int ronda, String revisorId,
                                        String decision, boolean tardia) {
        if (tardia) {
            registrar(evento, TipoEntrada.EVALUACION_TARDIA, preguntaId, revisorId,
                    Resumenes.evaluacionTardia(revisorId, ronda, decision), null);
        } else {
            registrar(evento, TipoEntrada.EVALUACION_ENVIADA, preguntaId, revisorId,
                    Resumenes.evaluacionEnviada(revisorId, ronda, decision), null);
        }
    }

    public void alCompletarseUnaRevision(EventoRecibido evento, String preguntaId, int ronda, boolean aprobada) {
        registrar(evento, TipoEntrada.REVISION_COMPLETADA, preguntaId, null,
                Resumenes.revisionCompletada(ronda, aprobada), null);
    }

    private void registrar(EventoRecibido evento, TipoEntrada tipo, String preguntaId, String actorId, String resumen,
                           Runnable efectoAdicional) {
        if (!procesados.registrarSiEsNuevo(evento.eventId())) {
            return;
        }
        if (efectoAdicional != null) {
            efectoAdicional.run();
        }
        historial.guardar(new EntradaDeHistorial(generadorDeId.get(), evento.eventId(), preguntaId, actorId, tipo,
                resumen, evento.detalles(), evento.ocurridaEn(), reloj.instant()));
    }

    // ------------------------------------------------------------------ consultas

    /**
     * El rastro de una pregunta tal como lo ve el observador: el administrador y los revisores de la pregunta
     * lo ven completo; su autor, solo lo que le corresponde.
     *
     * @throws HistorialNoEncontradoException si la pregunta no tiene historial o el observador no tiene relación con ella
     */
    @Transactional(readOnly = true)
    public List<EntradaDeHistorial> lineaDeTiempo(String preguntaId, Observador observador) {
        InfoPregunta info = preguntas.buscar(preguntaId).orElse(null);
        Set<TipoEntrada> tipos;
        if (observador.administrador()
                || (observador.revisor() && preguntas.esRevisor(preguntaId, observador.usuarioId()))) {
            tipos = null;
        } else if (observador.autor() && info != null && info.autorId().equals(observador.usuarioId())) {
            tipos = VISIBLES_PARA_EL_AUTOR;
        } else {
            throw new HistorialNoEncontradoException(preguntaId);
        }
        List<EntradaDeHistorial> entradas = historial.lineaDeTiempo(preguntaId, tipos);
        if (entradas.isEmpty() && info == null) {
            throw new HistorialNoEncontradoException(preguntaId);
        }
        return entradas;
    }

    /** Todo lo que el observador puede ver de sus preguntas, lo más reciente primero. */
    @Transactional(readOnly = true)
    public Pagina<EntradaDeHistorial> visiblesPara(Observador observador, int pagina, int tamano) {
        validarPagina(pagina, tamano);
        return corregir(pagina, historial.visiblesPara(observador, pagina, tamano),
                p -> historial.visiblesPara(observador, p, tamano));
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
    private static <T> Pagina<T> corregir(int pagina, Pagina<T> resultado, IntFunction<Pagina<T>> buscar) {
        return pagina > resultado.totalPaginas() ? buscar.apply(resultado.totalPaginas()) : resultado;
    }
}
