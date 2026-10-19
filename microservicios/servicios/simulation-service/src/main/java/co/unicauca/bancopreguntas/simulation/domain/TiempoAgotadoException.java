package co.unicauca.bancopreguntas.simulation.domain;

/** El tiempo máximo del simulacro ya terminó (RF-23): no se aceptan más respuestas. */
public class TiempoAgotadoException extends RuntimeException {

    public TiempoAgotadoException() {
        super("El tiempo del simulacro terminó: ya no se aceptan respuestas. El simulacro se calificó con lo contestado.");
    }
}
