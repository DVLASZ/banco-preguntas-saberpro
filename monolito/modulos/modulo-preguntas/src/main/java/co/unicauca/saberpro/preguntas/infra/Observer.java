package co.unicauca.saberpro.preguntas.infra;

/**
 * Patrón Observer (GoF): un observador que reacciona cuando el
 * {@link Subject} que vigila cambia de estado.
 */
public interface Observer {

    /** Invocado por el {@link Subject} cuando su estado cambió. */
    void actualizar();
}
