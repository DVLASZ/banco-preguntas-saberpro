package co.unicauca.bancopreguntas.simulation.infrastructure.config;

import co.unicauca.bancopreguntas.simulation.application.SimulationApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cierra periódicamente los simulacros cuyo tiempo se agotó (RF-23), aunque el estudiante no vuelva a consultarlos,
 * para que su resultado llegue igual al seguimiento académico.
 */
@Component
@ConditionalOnProperty(name = "simulation.expiration.enabled", havingValue = "true", matchIfMissing = true)
public class ExpirationScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ExpirationScheduler.class);

    private final SimulationApplicationService simulacros;

    public ExpirationScheduler(SimulationApplicationService simulacros) {
        this.simulacros = simulacros;
    }

    @Scheduled(fixedDelayString = "${simulation.expiration-interval-ms:30000}")
    void cerrarVencidos() {
        try {
            int cerrados = simulacros.cerrarVencidos();
            if (cerrados > 0) {
                LOG.info("Se cerraron {} simulacro(s) por tiempo agotado", cerrados);
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron cerrar los simulacros vencidos: {}", e.getMessage());
        }
    }
}
