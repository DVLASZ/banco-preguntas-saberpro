package co.unicauca.bancopreguntas.notification.application;

import co.unicauca.bancopreguntas.notification.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.notification.application.port.NotificationRepository;
import co.unicauca.bancopreguntas.notification.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.notification.application.port.QuestionInfoRepository;
import co.unicauca.bancopreguntas.notification.domain.InfoPregunta;
import co.unicauca.bancopreguntas.notification.domain.NotificacionNoEncontradaException;
import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.ObservacionDeRevisor;
import co.unicauca.bancopreguntas.notification.domain.Pagina;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;
import co.unicauca.bancopreguntas.notification.domain.aviso.ContextoDeAviso;
import co.unicauca.bancopreguntas.notification.domain.aviso.CreadoresDeAviso;
import co.unicauca.bancopreguntas.notification.domain.event.NotificationCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Casos de uso del servicio de notificaciones. Reacciona a los eventos del servicio de preguntas y del de
 * revisión creando avisos en la bandeja de los usuarios y publica {@code notification.created} para que el
 * {@code email-service} envíe el correo. Todos los manejadores de eventos son idempotentes.
 */
@Transactional
public class NotificationApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationApplicationService.class);

    private final NotificationRepository notificaciones;
    private final QuestionInfoRepository preguntas;
    private final DomainEventPublisher eventos;
    private final ProcessedEvents procesados;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;

    public NotificationApplicationService(NotificationRepository notificaciones, QuestionInfoRepository preguntas,
                                          DomainEventPublisher eventos, ProcessedEvents procesados, Clock reloj,
                                          Supplier<String> generadorDeId) {
        this.notificaciones = notificaciones;
        this.preguntas = preguntas;
        this.eventos = eventos;
        this.procesados = procesados;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
    }

    // ------------------------------------------------------------------ reacción a eventos

    /** {@code question.submitted}: recuerda el título y el autor para redactar los avisos siguientes. */
    public void alEnviarseUnaPregunta(String eventId, String preguntaId, String autorId, String titulo) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        String administrador = preguntas.buscar(preguntaId).map(InfoPregunta::administradorId).orElse(null);
        preguntas.guardar(new InfoPregunta(preguntaId, titulo, autorId, administrador));
    }

    /**
     * {@code review.reviewers-assigned}: avisa a cada revisor asignado y al autor de que su pregunta entró en
     * revisión, y recuerda qué administrador asignó para avisarle después.
     *
     * @param asignadoPor el administrador que asignó; {@code "system"} o {@code null} si fue una reasignación automática
     */
    public void alAsignarseRevisores(String eventId, String preguntaId, String autorId, int ronda,
                                     List<String> revisores, boolean automatica, String asignadoPor) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        InfoPregunta info = preguntas.buscar(preguntaId).orElse(null);
        if (info != null && asignadoPor != null && !asignadoPor.isBlank() && !"system".equals(asignadoPor)) {
            preguntas.guardar(info.conAdministrador(asignadoPor));
        }
        ContextoDeAviso contexto = ContextoDeAviso.de(info == null ? null : info.titulo(), preguntaId)
                .enRonda(ronda).asignadaAutomaticamente(automatica);
        revisores.forEach(revisor -> notificar(revisor, TipoNotificacion.REVISORES_ASIGNADOS, contexto));
        if (autorId != null && !autorId.isBlank()) {
            notificar(autorId, TipoNotificacion.PREGUNTA_EN_REVISION, contexto);
        }
    }

    /** {@code review.completed}: avisa al autor del resultado, con las observaciones de los revisores. */
    public void alCompletarseUnaRevision(String eventId, String preguntaId, String autorId, int ronda,
                                         boolean aprobada, List<ObservacionDeRevisor> observaciones) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        ContextoDeAviso contexto = ContextoDeAviso.de(tituloDe(preguntaId), preguntaId)
                .enRonda(ronda).conObservaciones(observaciones).conResultado(aprobada);
        notificar(autorId, aprobada ? TipoNotificacion.REVISION_APROBADA : TipoNotificacion.REVISION_RECHAZADA, contexto);
        // el administrador y los revisores que evaluaron también reciben el resultado
        Set<String> otros = new LinkedHashSet<>();
        administradorDe(preguntaId).ifPresent(otros::add);
        observaciones.forEach(o -> otros.add(o.revisorId()));
        otros.remove(autorId);
        otros.stream().filter(id -> !id.isBlank())
                .forEach(id -> notificar(id, TipoNotificacion.RESULTADO_DE_REVISION, contexto));
    }

    /**
     * {@code review.evaluation-submitted}: avisa al administrador de cada evaluación y al autor solo cuando es una
     * observación tardía (las demás ya le llegan resumidas en el resultado de la ronda).
     */
    public void alEnviarseUnaEvaluacion(String eventId, String preguntaId, String autorId, int ronda,
                                        String revisorId, String decision, String observacion, boolean tardia) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        ContextoDeAviso contexto = ContextoDeAviso.de(tituloDe(preguntaId), preguntaId).enRonda(ronda)
                .conEvaluacion(revisorId, decision, observacion);
        if (tardia) {
            notificar(autorId, TipoNotificacion.OBSERVACION_TARDIA, contexto);
        } else {
            // al autor le llega resumida en el resultado de la ronda; el administrador la sigue en el momento
            administradorDe(preguntaId).ifPresent(admin -> notificar(admin, TipoNotificacion.EVALUACION_RECIBIDA, contexto));
        }
    }

    /** {@code question.published}: avisa al autor, que se conoce por el envío a revisión. */
    public void alPublicarseUnaPregunta(String eventId, String preguntaId) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        InfoPregunta info = preguntas.buscar(preguntaId).orElse(null);
        if (info == null) {
            LOG.warn("Se publicó la pregunta {} pero no se conoce a su autor; no se envía el aviso", preguntaId);
            return;
        }
        notificar(info.autorId(), TipoNotificacion.PREGUNTA_PUBLICADA, ContextoDeAviso.de(info.titulo(), preguntaId));
    }

    // ------------------------------------------------------------------ bandeja

    @Transactional(readOnly = true)
    public Pagina<Notificacion> bandeja(String usuarioId, boolean soloNoLeidas, int pagina, int tamano) {
        validarPagina(pagina, tamano);
        Pagina<Notificacion> resultado = notificaciones.delUsuario(usuarioId, soloNoLeidas, pagina, tamano);
        return corregir(pagina, resultado, p -> notificaciones.delUsuario(usuarioId, soloNoLeidas, p, tamano));
    }

    @Transactional(readOnly = true)
    public long noLeidas(String usuarioId) {
        return notificaciones.contarNoLeidas(usuarioId);
    }

    /**
     * Marca una notificación como leída.
     *
     * @throws NotificacionNoEncontradaException si no existe o es de otro usuario
     */
    public Notificacion marcarComoLeida(String id, String usuarioId) {
        Notificacion actual = notificaciones.buscarPorId(id)
                .filter(n -> n.destinatarioId().equals(usuarioId))
                .orElseThrow(() -> new NotificacionNoEncontradaException(id));
        Notificacion leida = actual.marcadaComoLeida(reloj.instant());
        if (leida != actual) {
            notificaciones.guardar(leida);
        }
        return leida;
    }

    /** Marca como leídas todas las notificaciones del usuario y devuelve cuántas cambiaron. */
    public int marcarTodasComoLeidas(String usuarioId) {
        return notificaciones.marcarTodasComoLeidas(usuarioId, reloj.instant());
    }

    // ------------------------------------------------------------------ auxiliares

    private Optional<String> administradorDe(String preguntaId) {
        return preguntas.buscar(preguntaId).map(InfoPregunta::administradorId).filter(id -> !id.isBlank());
    }

    private String tituloDe(String preguntaId) {
        return preguntas.buscar(preguntaId).map(InfoPregunta::titulo).orElse(null);
    }

    /**
     * Crea la notificación con el creador (Factory Method) que corresponde al tipo, la deja en la bandeja y publica
     * {@code notification.created}; el envío del correo es cosa del {@code email-service} y no bloquea este flujo.
     */
    private void notificar(String destinatarioId, TipoNotificacion tipo, ContextoDeAviso contexto) {
        Notificacion notificacion = CreadoresDeAviso.para(tipo).crear(generadorDeId.get(), destinatarioId, contexto,
                reloj.instant());
        notificaciones.guardar(notificacion);
        eventos.publicar(new NotificationCreated(notificacion.id(), notificacion.destinatarioId(), tipo.name(),
                notificacion.preguntaId(), notificacion.asunto(), notificacion.mensaje(), notificacion.creadaEn()));
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
    private static <T> Pagina<T> corregir(int pagina, Pagina<T> resultado,
                                          java.util.function.IntFunction<Pagina<T>> buscar) {
        return pagina > resultado.totalPaginas() ? buscar.apply(resultado.totalPaginas()) : resultado;
    }
}
