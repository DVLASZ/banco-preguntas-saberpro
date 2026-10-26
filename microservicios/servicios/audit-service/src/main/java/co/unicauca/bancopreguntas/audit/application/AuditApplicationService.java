package co.unicauca.bancopreguntas.audit.application;

import co.unicauca.bancopreguntas.audit.application.port.AuditRepository;
import co.unicauca.bancopreguntas.audit.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.audit.domain.EntradaDeAuditoria;
import co.unicauca.bancopreguntas.audit.domain.EventoRecibido;
import co.unicauca.bancopreguntas.audit.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.audit.domain.Pagina;
import co.unicauca.bancopreguntas.audit.domain.Resumenes;
import co.unicauca.bancopreguntas.audit.domain.TipoEntrada;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;

/**
 * Casos de uso de la auditoría: registra cada hecho que anuncian los demás servicios (de las preguntas, de su revisión
 * y de la seguridad) y lo consulta el administrador. Los manejadores de eventos son idempotentes.
 */
@Transactional
public class AuditApplicationService {

    private final AuditRepository auditoria;
    private final ProcessedEvents procesados;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;

    public AuditApplicationService(AuditRepository auditoria, ProcessedEvents procesados, Clock reloj,
                                   Supplier<String> generadorDeId) {
        this.auditoria = auditoria;
        this.procesados = procesados;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
    }

    // ------------------------------------------------------------------ registro de eventos

    public void alEnviarseUnaPregunta(EventoRecibido evento, String preguntaId, String autorId, String titulo, int version) {
        registrar(evento, TipoEntrada.PREGUNTA_ENVIADA, preguntaId, autorId, Resumenes.preguntaEnviada(autorId, titulo, version));
    }

    public void alReabrirseUnaPregunta(EventoRecibido evento, String preguntaId, String autorId, int version) {
        registrar(evento, TipoEntrada.PREGUNTA_REABIERTA, preguntaId, autorId, Resumenes.preguntaReabierta(autorId, version));
    }

    public void alPublicarseUnaPregunta(EventoRecibido evento, String preguntaId, String publicadaPor) {
        registrar(evento, TipoEntrada.PREGUNTA_PUBLICADA, preguntaId, publicadaPor, Resumenes.preguntaPublicada(publicadaPor));
    }

    public void alCambiarDeEstado(EventoRecibido evento, String preguntaId, String desde, String hacia, String cambiadoPor) {
        registrar(evento, TipoEntrada.CAMBIO_DE_ESTADO, preguntaId, cambiadoPor, Resumenes.cambioDeEstado(desde, hacia, cambiadoPor));
    }

    public void alAsignarseRevisores(EventoRecibido evento, String preguntaId, int ronda, List<String> revisores,
                                     String asignadoPor, boolean automatica) {
        registrar(evento, TipoEntrada.REVISORES_ASIGNADOS, preguntaId, asignadoPor,
                Resumenes.revisoresAsignados(asignadoPor, ronda, revisores, automatica));
    }

    public void alEnviarseUnaEvaluacion(EventoRecibido evento, String preguntaId, int ronda, String revisorId,
                                        String decision, boolean tardia) {
        if (tardia) {
            registrar(evento, TipoEntrada.EVALUACION_TARDIA, preguntaId, revisorId, Resumenes.evaluacionTardia(revisorId, ronda, decision));
        } else {
            registrar(evento, TipoEntrada.EVALUACION_ENVIADA, preguntaId, revisorId, Resumenes.evaluacionEnviada(revisorId, ronda, decision));
        }
    }

    public void alCompletarseUnaRevision(EventoRecibido evento, String preguntaId, int ronda, boolean aprobada) {
        registrar(evento, TipoEntrada.REVISION_COMPLETADA, preguntaId, null, Resumenes.revisionCompletada(ronda, aprobada));
    }

    /** Un inicio de sesión (exitoso o fallido), para la auditoría de seguridad. */
    public void alIniciarSesion(EventoRecibido evento, String usuarioId, boolean exitoso) {
        registrar(evento, exitoso ? TipoEntrada.INICIO_DE_SESION : TipoEntrada.INICIO_DE_SESION_FALLIDO, null, usuarioId,
                exitoso ? Resumenes.inicioDeSesion(usuarioId) : Resumenes.inicioDeSesionFallido(usuarioId));
    }

    private void registrar(EventoRecibido evento, TipoEntrada tipo, String preguntaId, String actorId, String resumen) {
        if (!procesados.registrarSiEsNuevo(evento.eventId())) {
            return;
        }
        auditoria.guardar(new EntradaDeAuditoria(generadorDeId.get(), evento.eventId(), preguntaId, actorId, tipo, resumen,
                evento.detalles(), evento.ocurridaEn(), reloj.instant()));
    }

    // ------------------------------------------------------------------ consulta

    /** La auditoría completa (incluidos los inicios de sesión), con filtros opcionales, lo más reciente primero. */
    @Transactional(readOnly = true)
    public Pagina<EntradaDeAuditoria> consultar(FiltroAuditoria filtro, int pagina, int tamano) {
        if (tamano < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        }
        if (pagina < 1) {
            throw new IllegalArgumentException("La página debe ser al menos 1");
        }
        Pagina<EntradaDeAuditoria> resultado = auditoria.consultar(filtro, pagina, tamano);
        return pagina > resultado.totalPaginas() ? auditoria.consultar(filtro, resultado.totalPaginas(), tamano) : resultado;
    }
}
