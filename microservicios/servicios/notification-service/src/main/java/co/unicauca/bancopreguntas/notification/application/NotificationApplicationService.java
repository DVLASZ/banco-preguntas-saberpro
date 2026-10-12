package co.unicauca.bancopreguntas.notification.application;

import co.unicauca.bancopreguntas.notification.application.port.EmailSender;
import co.unicauca.bancopreguntas.notification.application.port.NotificationRepository;
import co.unicauca.bancopreguntas.notification.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.notification.application.port.QuestionInfoRepository;
import co.unicauca.bancopreguntas.notification.domain.InfoPregunta;
import co.unicauca.bancopreguntas.notification.domain.Mensaje;
import co.unicauca.bancopreguntas.notification.domain.NotificacionNoEncontradaException;
import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.ObservacionDeRevisor;
import co.unicauca.bancopreguntas.notification.domain.Pagina;
import co.unicauca.bancopreguntas.notification.domain.Plantillas;
import co.unicauca.bancopreguntas.notification.domain.TipoNotificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Casos de uso del servicio de notificaciones. Reacciona a los eventos del servicio de preguntas y del de
 * revisión creando avisos para los usuarios, los envía por correo (simulado) y atiende la bandeja de cada uno.
 * Todos los manejadores de eventos son idempotentes.
 */
@Transactional
public class NotificationApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationApplicationService.class);

    private final NotificationRepository notificaciones;
    private final QuestionInfoRepository preguntas;
    private final EmailSender correo;
    private final ProcessedEvents procesados;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;
    private final Function<String, String> correoDe;

    /**
     * @param correoDe obtiene la dirección de correo de un usuario a partir de su identificador
     */
    public NotificationApplicationService(NotificationRepository notificaciones, QuestionInfoRepository preguntas,
                                          EmailSender correo, ProcessedEvents procesados, Clock reloj,
                                          Supplier<String> generadorDeId, Function<String, String> correoDe) {
        this.notificaciones = notificaciones;
        this.preguntas = preguntas;
        this.correo = correo;
        this.procesados = procesados;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
        this.correoDe = correoDe;
    }

    // ------------------------------------------------------------------ reacción a eventos

    /** {@code question.submitted}: recuerda el título y el autor para redactar los avisos siguientes. */
    public void alEnviarseUnaPregunta(String eventId, String preguntaId, String autorId, String titulo) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        preguntas.guardar(new InfoPregunta(preguntaId, titulo, autorId));
    }

    /** {@code review.reviewers-assigned}: avisa a cada revisor asignado. */
    public void alAsignarseRevisores(String eventId, String preguntaId, int ronda, List<String> revisores,
                                     boolean automatica) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        String titulo = tituloDe(preguntaId);
        Mensaje mensaje = Plantillas.revisoresAsignados(titulo, preguntaId, ronda, automatica);
        revisores.forEach(revisor -> notificar(revisor, TipoNotificacion.REVISORES_ASIGNADOS, preguntaId, mensaje));
    }

    /** {@code review.completed}: avisa al autor del resultado, con las observaciones de los revisores. */
    public void alCompletarseUnaRevision(String eventId, String preguntaId, String autorId, int ronda,
                                         boolean aprobada, List<ObservacionDeRevisor> observaciones) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        String titulo = tituloDe(preguntaId);
        if (aprobada) {
            notificar(autorId, TipoNotificacion.REVISION_APROBADA, preguntaId,
                    Plantillas.revisionAprobada(titulo, preguntaId, ronda, observaciones));
        } else {
            notificar(autorId, TipoNotificacion.REVISION_RECHAZADA, preguntaId,
                    Plantillas.revisionRechazada(titulo, preguntaId, ronda, observaciones));
        }
    }

    /**
     * {@code review.evaluation-submitted}: solo interesa al autor cuando es una observación tardía (las demás
     * ya le llegan resumidas en el resultado de la ronda).
     */
    public void alEnviarseUnaEvaluacion(String eventId, String preguntaId, String autorId, int ronda,
                                        String revisorId, String decision, String observacion, boolean tardia) {
        if (!procesados.registrarSiEsNuevo(eventId) || !tardia) {
            return;
        }
        notificar(autorId, TipoNotificacion.OBSERVACION_TARDIA, preguntaId,
                Plantillas.observacionTardia(tituloDe(preguntaId), preguntaId, ronda, revisorId, decision, observacion));
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
        notificar(info.autorId(), TipoNotificacion.PREGUNTA_PUBLICADA, preguntaId,
                Plantillas.preguntaPublicada(info.titulo(), preguntaId));
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

    /** Los correos enviados (simulados), para que el administrador compruebe qué salió. */
    @Transactional(readOnly = true)
    public Pagina<Notificacion> correosEnviados(int pagina, int tamano) {
        validarPagina(pagina, tamano);
        Pagina<Notificacion> resultado = notificaciones.conCorreoEnviado(pagina, tamano);
        return corregir(pagina, resultado, p -> notificaciones.conCorreoEnviado(p, tamano));
    }

    // ------------------------------------------------------------------ auxiliares

    private String tituloDe(String preguntaId) {
        return preguntas.buscar(preguntaId).map(InfoPregunta::titulo).orElse(null);
    }

    /** Crea la notificación y envía el correo. Si el envío falla, la notificación queda en la bandeja sin marca de envío. */
    private void notificar(String destinatarioId, TipoNotificacion tipo, String preguntaId, Mensaje mensaje) {
        Instant ahora = reloj.instant();
        Notificacion notificacion = Notificacion.nueva(generadorDeId.get(), destinatarioId,
                correoDe.apply(destinatarioId), tipo, preguntaId, mensaje, ahora);
        try {
            correo.enviar(notificacion.comoCorreo());
            notificacion = notificacion.conCorreoEnviado(reloj.instant());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo enviar el correo de la notificación {} a {}: {}", notificacion.id(),
                    notificacion.destinatarioCorreo(), e.getMessage());
        }
        notificaciones.guardar(notificacion);
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
