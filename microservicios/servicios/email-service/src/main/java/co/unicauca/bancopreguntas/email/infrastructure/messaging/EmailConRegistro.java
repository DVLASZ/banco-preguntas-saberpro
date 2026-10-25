package co.unicauca.bancopreguntas.email.infrastructure.messaging;

import co.unicauca.bancopreguntas.email.application.port.EmailSender;
import co.unicauca.bancopreguntas.email.domain.Correo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Patrón Decorator: deja constancia en el registro de cada envío, correcto o fallido, y cuenta cuántos se hicieron.
 * Envuelve a cualquier {@link EmailSender} sin que este ni los casos de uso lo sepan.
 */
public class EmailConRegistro implements EmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(EmailConRegistro.class);

    private final EmailSender decorado;
    private long enviados;
    private long fallidos;

    public EmailConRegistro(EmailSender decorado) {
        this.decorado = decorado;
    }

    @Override
    public void enviar(Correo correo) {
        try {
            decorado.enviar(correo);
            enviados++;
            LOG.info("Correo enviado a {} (asunto: {})", correo.para(), correo.asunto());
        } catch (RuntimeException e) {
            fallidos++;
            LOG.error("No se pudo enviar el correo a {}: {}", correo.para(), e.getMessage());
            throw e;
        }
    }

    public long enviados() {
        return enviados;
    }

    public long fallidos() {
        return fallidos;
    }
}
