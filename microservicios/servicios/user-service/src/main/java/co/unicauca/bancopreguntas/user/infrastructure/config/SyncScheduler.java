package co.unicauca.bancopreguntas.user.infrastructure.config;

import co.unicauca.bancopreguntas.user.application.ResultadoDeSincronizacion;
import co.unicauca.bancopreguntas.user.application.UserApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Mantiene al día la copia de usuarios y publica los inicios de sesión de Keycloak. Si Keycloak no responde,
 * lo registra y reintenta en el siguiente ciclo: el servicio sigue atendiendo con lo que ya tiene.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "user.sync.enabled", havingValue = "true", matchIfMissing = true)
public class SyncScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(SyncScheduler.class);

    private final UserApplicationService usuarios;

    public SyncScheduler(UserApplicationService usuarios) {
        this.usuarios = usuarios;
    }

    @Scheduled(initialDelayString = "${user.sync.initial-delay:PT10S}", fixedDelayString = "${user.sync.users-interval:PT1M}")
    void sincronizarUsuarios() {
        try {
            ResultadoDeSincronizacion r = usuarios.sincronizarUsuarios();
            if (r.creados() + r.desactivados() > 0) {
                LOG.info("Usuarios sincronizados con Keycloak: {} nuevos, {} actualizados, {} desactivados",
                        r.creados(), r.actualizados(), r.desactivados());
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron sincronizar los usuarios con Keycloak: {}", e.getMessage());
        }
    }

    @Scheduled(initialDelayString = "${user.sync.initial-delay:PT10S}", fixedDelayString = "${user.sync.sessions-interval:PT5S}")
    void sincronizarSesiones() {
        try {
            int publicados = usuarios.sincronizarSesiones();
            if (publicados > 0) {
                LOG.info("Se publicaron {} inicios de sesión de Keycloak", publicados);
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron leer los inicios de sesión de Keycloak: {}", e.getMessage());
        }
    }
}
