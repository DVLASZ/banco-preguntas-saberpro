package co.unicauca.bancopreguntas.notification.infrastructure.messaging;

import co.unicauca.bancopreguntas.notification.application.port.EmailSender;
import co.unicauca.bancopreguntas.notification.domain.Correo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de correo que simula el envío: lo deja en el registro del servicio. Por el diseño del puerto,
 * sustituirlo por un servidor SMTP real no toca la aplicación.
 */
@Component
public class SimulatedEmailSender implements EmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(SimulatedEmailSender.class);

    @Override
    public void enviar(Correo correo) {
        LOG.info("[correo simulado] Para: {} | Asunto: {}", correo.para(), correo.asunto());
        LOG.debug("[correo simulado] Cuerpo:\n{}", correo.cuerpo());
    }
}
