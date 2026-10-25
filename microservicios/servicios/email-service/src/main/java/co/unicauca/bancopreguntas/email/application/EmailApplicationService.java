package co.unicauca.bancopreguntas.email.application;

import co.unicauca.bancopreguntas.email.application.port.EmailSender;
import co.unicauca.bancopreguntas.email.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.email.application.port.SentEmailRepository;
import co.unicauca.bancopreguntas.email.domain.Correo;
import co.unicauca.bancopreguntas.email.domain.CorreoEnviado;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;
import co.unicauca.bancopreguntas.email.domain.Pagina;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Casos de uso del servicio de correo: por cada notificación creada arma el correo, lo envía (simulado) y deja el
 * registro de cómo salió. Un envío fallido no se reintenta reencolando el mensaje: queda registrado como fallido.
 */
@Transactional
public class EmailApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailApplicationService.class);

    private final EmailSender correo;
    private final SentEmailRepository enviados;
    private final ProcessedEvents procesados;
    private final Clock reloj;
    private final Supplier<String> generadorDeId;
    private final Function<String, String> correoDe;

    /** @param correoDe obtiene la dirección de correo de un usuario a partir de su identificador */
    public EmailApplicationService(EmailSender correo, SentEmailRepository enviados, ProcessedEvents procesados, Clock reloj,
                                   Supplier<String> generadorDeId, Function<String, String> correoDe) {
        this.correo = correo;
        this.enviados = enviados;
        this.procesados = procesados;
        this.reloj = reloj;
        this.generadorDeId = generadorDeId;
        this.correoDe = correoDe;
    }

    /** {@code notification.created}: envía el correo de la notificación y registra el resultado. Idempotente. */
    public void alCrearseUnaNotificacion(String eventId, String notificacionId, String destinatarioId, String asunto,
                                         String cuerpo) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        Correo mensaje = new Correo(correoDe.apply(destinatarioId), asunto, cuerpo);
        try {
            correo.enviar(mensaje);
            enviados.guardar(CorreoEnviado.enviado(generadorDeId.get(), notificacionId, destinatarioId, mensaje, reloj.instant()));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo enviar el correo de la notificación {} a {}: {}", notificacionId, mensaje.para(), e.getMessage());
            enviados.guardar(CorreoEnviado.fallido(generadorDeId.get(), notificacionId, destinatarioId, mensaje, e.getMessage(),
                    reloj.instant()));
        }
    }

    /** Los correos registrados, el más reciente primero, para que el administrador compruebe qué salió. */
    @Transactional(readOnly = true)
    public Pagina<CorreoEnviado> correos(EstadoCorreo estado, int pagina, int tamano) {
        if (tamano < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        }
        if (pagina < 1) {
            throw new IllegalArgumentException("La página debe ser al menos 1");
        }
        Pagina<CorreoEnviado> resultado = enviados.listar(estado, pagina, tamano);
        return pagina > resultado.totalPaginas() ? enviados.listar(estado, resultado.totalPaginas(), tamano) : resultado;
    }
}
