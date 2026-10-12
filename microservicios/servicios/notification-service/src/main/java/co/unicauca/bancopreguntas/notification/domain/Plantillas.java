package co.unicauca.bancopreguntas.notification.domain;

import java.util.List;

/** Redacta el asunto y el cuerpo de cada tipo de aviso. */
public final class Plantillas {

    private Plantillas() {
    }

    private static String nombre(String titulo, String preguntaId) {
        return titulo == null || titulo.isBlank() ? "la pregunta " + preguntaId : "«" + titulo + "»";
    }

    public static Mensaje revisoresAsignados(String titulo, String preguntaId, int ronda, boolean automatica) {
        String pregunta = nombre(titulo, preguntaId);
        String motivo = automatica
                ? "El autor corrigió la pregunta y la envió de nuevo; como ya la habías revisado, quedas asignado otra vez "
                        + "(ronda " + ronda + ")."
                : "El administrador te asignó como revisor (ronda " + ronda + ").";
        return new Mensaje("Pregunta por revisar: " + pregunta,
                "Tienes pendiente evaluar " + pregunta + ". " + motivo
                        + " Entra a tu bandeja de revisión para aprobarla o rechazarla.");
    }

    public static Mensaje revisionAprobada(String titulo, String preguntaId, int ronda, List<ObservacionDeRevisor> observaciones) {
        String pregunta = nombre(titulo, preguntaId);
        StringBuilder cuerpo = new StringBuilder("Los revisores aprobaron " + pregunta + " (ronda " + ronda
                + "). Queda lista para que el administrador la publique.");
        agregarObservaciones(cuerpo, observaciones);
        return new Mensaje("Pregunta aprobada: " + pregunta, cuerpo.toString());
    }

    public static Mensaje revisionRechazada(String titulo, String preguntaId, int ronda, List<ObservacionDeRevisor> observaciones) {
        String pregunta = nombre(titulo, preguntaId);
        StringBuilder cuerpo = new StringBuilder("Un revisor rechazó " + pregunta + " (ronda " + ronda
                + "). Puedes reabrirla, corregirla y enviarla de nuevo; los demás revisores aún pueden dejar sus observaciones.");
        agregarObservaciones(cuerpo, observaciones);
        return new Mensaje("Pregunta rechazada: " + pregunta, cuerpo.toString());
    }

    public static Mensaje observacionTardia(String titulo, String preguntaId, int ronda, String revisorId,
                                            String decision, String observacion) {
        String pregunta = nombre(titulo, preguntaId);
        StringBuilder cuerpo = new StringBuilder("El revisor " + revisorId + " dejó su evaluación (" + decision
                + ") sobre " + pregunta + " en la ronda " + ronda + ", que ya había sido rechazada.");
        if (observacion != null && !observacion.isBlank()) {
            cuerpo.append("\nObservación: ").append(observacion);
        }
        return new Mensaje("Nueva observación sobre " + pregunta, cuerpo.toString());
    }

    public static Mensaje preguntaPublicada(String titulo, String preguntaId) {
        String pregunta = nombre(titulo, preguntaId);
        return new Mensaje("Pregunta publicada: " + pregunta,
                "El administrador publicó " + pregunta + ". Ya hace parte del banco de preguntas.");
    }

    private static void agregarObservaciones(StringBuilder cuerpo, List<ObservacionDeRevisor> observaciones) {
        List<ObservacionDeRevisor> conTexto = observaciones.stream()
                .filter(o -> o.texto() != null && !o.texto().isBlank()).toList();
        if (conTexto.isEmpty()) {
            return;
        }
        cuerpo.append("\nObservaciones:");
        conTexto.forEach(o -> cuerpo.append("\n- ").append(o.revisorId()).append(" (").append(o.decision())
                .append("): ").append(o.texto()));
    }
}
