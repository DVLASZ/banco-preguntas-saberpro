package co.unicauca.bancopreguntas.simulation.domain;

/** El usuario intentó usar un simulacro que es de otro estudiante. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
