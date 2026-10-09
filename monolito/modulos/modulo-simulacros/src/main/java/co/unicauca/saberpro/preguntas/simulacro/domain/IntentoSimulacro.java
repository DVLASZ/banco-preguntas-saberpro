package co.unicauca.saberpro.preguntas.simulacro.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Un intento de un Estudiante de presentar un {@link Simulacro} (HU-13):
 * guarda las respuestas que va seleccionando y, al finalizar, su
 * calificación automática (HU-14, RF-24).
 */
public class IntentoSimulacro {

    private final String id;
    private final String simulacroId;
    private final String estudianteUsername;
    private final Map<String, Character> respuestas = new HashMap<>();
    private EstadoIntento estado;
    private Integer aciertos;

    public IntentoSimulacro(String id, String simulacroId, String estudianteUsername) {
        this.id = Objects.requireNonNull(id, "El id del intento es obligatorio");
        this.simulacroId = Objects.requireNonNull(simulacroId, "El simulacro del intento es obligatorio");
        this.estudianteUsername = Objects.requireNonNull(estudianteUsername, "El estudiante del intento es obligatorio");
        this.estado = EstadoIntento.EN_CURSO;
    }

    public String getId() {
        return id;
    }

    public String getSimulacroId() {
        return simulacroId;
    }

    public String getEstudianteUsername() {
        return estudianteUsername;
    }

    public EstadoIntento getEstado() {
        return estado;
    }

    public Integer getAciertos() {
        return aciertos;
    }

    /** Respuesta seleccionada por el estudiante para una pregunta, o {@code null} si no la ha respondido. */
    public Character getRespuesta(String preguntaId) {
        return respuestas.get(preguntaId);
    }

    public Map<String, Character> getRespuestas() {
        return Map.copyOf(respuestas);
    }

    public void responder(String preguntaId, char respuesta) {
        if (estado != EstadoIntento.EN_CURSO) {
            throw new IllegalStateException("No se puede responder un intento que ya finalizó");
        }
        respuestas.put(preguntaId, Character.toUpperCase(respuesta));
    }

    public void finalizar(int aciertos) {
        if (estado != EstadoIntento.EN_CURSO) {
            throw new IllegalStateException("El intento ya estaba finalizado");
        }
        this.aciertos = aciertos;
        this.estado = EstadoIntento.FINALIZADO;
    }
}
