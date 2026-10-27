package co.unicauca.bancopreguntas.lifecycle.domain;

/** El usuario no es el autor de la pregunta que intenta modificar o enviar. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
