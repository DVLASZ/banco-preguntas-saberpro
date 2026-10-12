package co.unicauca.bancopreguntas.notification.domain;

/** La notificación no existe o no pertenece al usuario que la pide (no se distingue, para no revelar nada). */
public class NotificacionNoEncontradaException extends RuntimeException {

    public NotificacionNoEncontradaException(String id) {
        super("No existe la notificación " + id);
    }
}
