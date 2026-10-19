package co.unicauca.bancopreguntas.simulation.application;

import co.unicauca.bancopreguntas.simulation.domain.Simulacro;

/** Un simulacro junto con el tiempo que le queda en el momento de consultarlo. */
public record VistaDeSimulacro(Simulacro simulacro, long segundosRestantes) {
}
