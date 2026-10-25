package co.unicauca.bancopreguntas.notification.infrastructure.persistence;

import co.unicauca.bancopreguntas.notification.application.port.NotificationRepository;
import co.unicauca.bancopreguntas.notification.domain.Notificacion;
import co.unicauca.bancopreguntas.notification.domain.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/** Adaptador de persistencia del puerto {@link NotificationRepository}. */
@Component
public class JpaNotificationRepository implements NotificationRepository {

    private final NotificationJpaRepository repositorio;

    public JpaNotificationRepository(NotificationJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(Notificacion notificacion) {
        NotificationEntity entidad = repositorio.findById(notificacion.id()).orElseGet(() -> NotificationEntity.de(notificacion));
        entidad.copiar(notificacion);
        repositorio.saveAndFlush(entidad);
    }

    @Override
    public Optional<Notificacion> buscarPorId(String id) {
        return repositorio.findById(id).map(NotificationEntity::aDominio);
    }

    @Override
    public Pagina<Notificacion> delUsuario(String usuarioId, boolean soloNoLeidas, int pagina, int tamano) {
        PageRequest pedido = PageRequest.of(pagina - 1, tamano);
        Page<NotificationEntity> resultado = soloNoLeidas
                ? repositorio.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDescIdAsc(usuarioId, pedido)
                : repositorio.findByRecipientIdOrderByCreatedAtDescIdAsc(usuarioId, pedido);
        return aPagina(resultado, pagina, tamano);
    }

    @Override
    public long contarNoLeidas(String usuarioId) {
        return repositorio.countByRecipientIdAndReadAtIsNull(usuarioId);
    }

    @Override
    public int marcarTodasComoLeidas(String usuarioId, Instant ahora) {
        return repositorio.marcarTodasComoLeidas(usuarioId, ahora);
    }

    private static Pagina<Notificacion> aPagina(Page<NotificationEntity> resultado, int pagina, int tamano) {
        return new Pagina<>(resultado.getContent().stream().map(NotificationEntity::aDominio).toList(), pagina, tamano,
                (int) resultado.getTotalElements());
    }
}
