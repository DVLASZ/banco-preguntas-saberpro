package co.unicauca.bancopreguntas.lifecycle.domain;

import java.util.Set;

/** Aprobada: lista para que el administrador la publique. */
public final class FaseAprobada implements FaseDePregunta {

    @Override
    public EstadoPregunta estado() {
        return EstadoPregunta.APROBADA;
    }

    @Override
    public Set<EstadoPregunta> destinos() {
        return Set.of(EstadoPregunta.PUBLICADA);
    }

    @Override
    public EstadoPregunta publicar() {
        return EstadoPregunta.PUBLICADA;
    }
}
