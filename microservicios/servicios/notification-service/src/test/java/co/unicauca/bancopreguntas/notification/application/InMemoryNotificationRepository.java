package co.unicauca.bancopreguntas.notification.application;

import co.unicauca.bancopreguntas.notification.application.port.NotificationRepository;
import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.Pagina;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba del repositorio de notificaciones, en memoria. */
class InMemoryNotificationRepository implements NotificationRepository {

    private final Map<String, Notificacion> todas = new LinkedHashMap<>();

    void vaciar() {
        todas.clear();
    }

    List<Notificacion> todas() {
        return List.copyOf(todas.values());
    }

    @Override
    public void guardar(Notificacion notificacion) {
        todas.put(notificacion.id(), notificacion);
    }

    @Override
    public Optional<Notificacion> buscarPorId(String id) {
        return Optional.ofNullable(todas.get(id));
    }

    @Override
    public Pagina<Notificacion> delUsuario(String usuarioId, boolean soloNoLeidas, int pagina, int tamano) {
        return paginar(todas.values().stream()
                .filter(n -> n.destinatarioId().equals(usuarioId))
                .filter(n -> !soloNoLeidas || !n.leida())
                .sorted(Comparator.comparing(Notificacion::creadaEn).reversed())
                .toList(), pagina, tamano);
    }

    @Override
    public long contarNoLeidas(String usuarioId) {
        return todas.values().stream().filter(n -> n.destinatarioId().equals(usuarioId) && !n.leida()).count();
    }

    @Override
    public int marcarTodasComoLeidas(String usuarioId, Instant ahora) {
        int[] cambiadas = {0};
        todas.replaceAll((id, n) -> {
            if (n.destinatarioId().equals(usuarioId) && !n.leida()) {
                cambiadas[0]++;
                return n.marcadaComoLeida(ahora);
            }
            return n;
        });
        return cambiadas[0];
    }

    private static Pagina<Notificacion> paginar(List<Notificacion> lista, int pagina, int tamano) {
        int desde = Math.min((pagina - 1) * tamano, lista.size());
        int hasta = Math.min(desde + tamano, lista.size());
        return new Pagina<>(lista.subList(desde, hasta), pagina, tamano, lista.size());
    }
}
