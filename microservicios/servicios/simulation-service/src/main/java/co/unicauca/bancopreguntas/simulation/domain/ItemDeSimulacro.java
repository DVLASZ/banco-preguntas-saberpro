package co.unicauca.bancopreguntas.simulation.domain;

/**
 * Una pregunta dentro de un simulacro: la copia que se le mostró al estudiante y lo que contestó.
 *
 * @param orden     posición en el simulacro, desde 1
 * @param respuesta la letra elegida, o {@code null} si no ha contestado
 */
public record ItemDeSimulacro(int orden, PreguntaDeCatalogo pregunta, Character respuesta) {

    public boolean respondida() {
        return respuesta != null;
    }

    public boolean acertada() {
        return respuesta != null && respuesta == pregunta.respuestaCorrecta();
    }

    public ItemDeSimulacro respondidoCon(char letra) {
        return new ItemDeSimulacro(orden, pregunta, Character.toUpperCase(letra));
    }
}
