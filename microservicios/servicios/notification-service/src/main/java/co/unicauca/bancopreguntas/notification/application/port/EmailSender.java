package co.unicauca.bancopreguntas.notification.application.port;

import co.unicauca.bancopreguntas.notification.domain.Correo;

/**
 * Puerto de salida: envío de correo electrónico. Hoy lo implementa un adaptador que solo simula el envío;
 * conectar un servidor SMTP real es cambiar ese adaptador, no la aplicación.
 */
public interface EmailSender {

    /** @throws RuntimeException si el correo no pudo enviarse */
    void enviar(Correo correo);
}
