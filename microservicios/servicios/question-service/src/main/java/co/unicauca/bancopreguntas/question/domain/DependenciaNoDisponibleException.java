package co.unicauca.bancopreguntas.question.domain;

/**
 * No se pudo consultar a otro servicio del que depende la operación (validación o ciclo de vida): estaba caído o
 * respondió con un error. La operación no se realizó y se puede reintentar.
 */
public class DependenciaNoDisponibleException extends RuntimeException {

    public DependenciaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
