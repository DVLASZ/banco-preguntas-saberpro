package co.unicauca.bancopreguntas.history.domain;

import java.util.List;

/** Redacta el resumen de una línea de cada tipo de entrada. */
public final class Resumenes {

    private Resumenes() {
    }

    private static String usuario(String id) {
        return id == null || id.isBlank() ? "el sistema" : id;
    }

    public static String preguntaEnviada(String autorId, String titulo, int version) {
        return usuario(autorId) + " envió a revisión" + nombre(titulo) + " (versión " + version + ")";
    }

    public static String preguntaReabierta(String autorId, int version) {
        return usuario(autorId) + " reabrió la pregunta para corregirla (nueva versión " + version + ")";
    }

    public static String preguntaPublicada(String publicadaPor) {
        return usuario(publicadaPor) + " publicó la pregunta";
    }

    public static String cambioDeEstado(String desde, String hacia, String cambiadoPor) {
        return "Estado: " + (desde == null ? "(nuevo)" : desde) + " → " + hacia + " (por " + usuario(cambiadoPor) + ")";
    }

    public static String revisoresAsignados(String asignadoPor, int ronda, List<String> revisores, boolean automatica) {
        String quienes = String.join(", ", revisores);
        return automatica
                ? "Se reasignaron automáticamente los mismos revisores en la ronda " + ronda + ": " + quienes
                : usuario(asignadoPor) + " asignó a " + quienes + " en la ronda " + ronda;
    }

    public static String evaluacionEnviada(String revisorId, int ronda, String decision) {
        return usuario(revisorId) + " evaluó la pregunta en la ronda " + ronda + ": " + decision;
    }

    public static String evaluacionTardia(String revisorId, int ronda, String decision) {
        return usuario(revisorId) + " dejó una observación tardía en la ronda " + ronda + ": " + decision;
    }

    public static String revisionCompletada(int ronda, boolean aprobada) {
        return "La ronda " + ronda + " terminó " + (aprobada ? "aprobada" : "rechazada");
    }

    public static String inicioDeSesion(String usuarioId) {
        return usuario(usuarioId) + " inició sesión";
    }

    public static String inicioDeSesionFallido(String usuarioId) {
        return "Intento fallido de inicio de sesión de " + usuario(usuarioId);
    }

    private static String nombre(String titulo) {
        return titulo == null || titulo.isBlank() ? " la pregunta" : " «" + titulo + "»";
    }
}
