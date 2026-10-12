package co.unicauca.bancopreguntas.notification.application;

import co.unicauca.bancopreguntas.notification.application.port.EmailSender;
import co.unicauca.bancopreguntas.notification.domain.Correo;

import java.util.ArrayList;
import java.util.List;

/** Doble de prueba del envío de correo: recuerda lo enviado y puede simular una caída del servidor. */
class RecordingEmailSender implements EmailSender {

    private final List<Correo> enviados = new ArrayList<>();
    boolean fallar;

    @Override
    public void enviar(Correo correo) {
        if (fallar) {
            throw new IllegalStateException("servidor de correo caído");
        }
        enviados.add(correo);
    }

    List<Correo> enviados() {
        return List.copyOf(enviados);
    }
}
