package co.unicauca.bancopreguntas.simulation.domain;

/** No existe un simulacro con ese identificador. */
public class SimulacroNoEncontradoException extends RuntimeException {

    public SimulacroNoEncontradoException(String id) {
        super("No existe el simulacro " + id);
    }
}
