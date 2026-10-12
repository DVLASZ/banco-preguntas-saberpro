package co.unicauca.bancopreguntas.notification.application.port;

import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.Pagina;

import java.time.Instant;
import java.util.Optional;

/** Puerto de salida: persistencia de las notificaciones. */
public interface NotificationRepository {

    /** Crea o actualiza la notificación. */
    void guardar(Notificacion notificacion);

    Optional<Notificacion> buscarPorId(String id);

    /** La bandeja de un usuario, la más reciente primero. */
    Pagina<Notificacion> delUsuario(String usuarioId, boolean soloNoLeidas, int pagina, int tamano);

    long contarNoLeidas(String usuarioId);

    /** Marca como leídas todas las del usuario y devuelve cuántas cambiaron. */
    int marcarTodasComoLeidas(String usuarioId, Instant ahora);

    /** Los correos ya enviados (simulados), el más reciente primero. */
    Pagina<Notificacion> conCorreoEnviado(int pagina, int tamano);
}
