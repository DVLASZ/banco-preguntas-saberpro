package co.unicauca.bancopreguntas.lifecycle.domain;

import java.util.Set;

/** Rechazada: el autor la puede reabrir para corregirla; vuelve a Borrador como una versión nueva. */
public final class FaseRechazada implements FaseDePregunta {

    @Override
    public EstadoPregunta estado() {
        return EstadoPregunta.RECHAZADA;
    }

    @Override
    public Set<EstadoPregunta> destinos() {
        return Set.of(EstadoPregunta.BORRADOR);
    }

    @Override
    public EstadoPregunta reabrir() {
        return EstadoPregunta.BORRADOR;
    }
}
