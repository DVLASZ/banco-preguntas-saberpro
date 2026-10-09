package co.unicauca.bancopreguntas.question.application.port;

/** Puerto de salida: registro de eventos ya procesados, para que los consumidores sean idempotentes. */
public interface ProcessedEvents {

    /**
     * Marca el evento como procesado.
     *
     * @return {@code true} si era la primera vez; {@code false} si ya se había procesado y debe ignorarse
     */
    boolean registrarSiEsNuevo(String eventId);
}
