package co.unicauca.saberpro.preguntas.infra;

/**
 * Patrón Observer (GoF): un sujeto observable al que se le pueden
 * suscribir/desuscribir {@link Observer} y que los notifica cuando su
 * estado cambia.
 */
public interface Subject {

    void agregarObservador(Observer observador);

    void eliminarObservador(Observer observador);

    void notificarObservadores();
}
