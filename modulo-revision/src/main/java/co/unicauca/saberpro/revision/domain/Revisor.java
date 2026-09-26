package co.unicauca.saberpro.revision.domain;

/** Un usuario con rol Revisor que puede recibir preguntas para evaluar. */
public record Revisor(String usuario, String nombreCompleto) {

    /** Correo simulado del revisor (no se envía nada real: RNF de alcance académico). */
    public String correo() {
        return usuario + "@unicauca.edu.co";
    }
}
