package co.unicauca.bancopreguntas.simulation.domain;

/** Los criterios del simulacro no son válidos (cantidad o duración fuera de rango). */
public class CriteriosInvalidosException extends RuntimeException {

    public CriteriosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
