package co.unicauca.bancopreguntas.email.infrastructure.messaging;

import co.unicauca.bancopreguntas.email.application.port.EmailSender;
import co.unicauca.bancopreguntas.email.domain.Correo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Patrón Decorator: le agrega reintentos a cualquier {@link EmailSender}. Un servidor de correo real falla de vez en
 * cuando por causas pasajeras; antes de dar el envío por fallido se intenta de nuevo. Respeta el contrato del puerto:
 * si tras todos los intentos sigue fallando, lanza la última excepción.
 */
public class EmailConReintentos implements EmailSender {

    private static final Logger LOG = LoggerFactory.getLogger(EmailConReintentos.class);

    private final EmailSender decorado;
    private final int intentos;

    /** @param intentos cuántas veces se intenta enviar como máximo, al menos 1 */
    public EmailConReintentos(EmailSender decorado, int intentos) {
        if (intentos < 1) {
            throw new IllegalArgumentException("Los intentos deben ser al menos 1");
        }
        this.decorado = decorado;
        this.intentos = intentos;
    }

    @Override
    public void enviar(Correo correo) {
        RuntimeException ultimo = null;
        for (int intento = 1; intento <= intentos; intento++) {
            try {
                decorado.enviar(correo);
                return;
            } catch (RuntimeException e) {
                ultimo = e;
                LOG.warn("Falló el intento {} de {} de enviar el correo a {}: {}", intento, intentos, correo.para(),
                        e.getMessage());
            }
        }
        throw ultimo;
    }
}
