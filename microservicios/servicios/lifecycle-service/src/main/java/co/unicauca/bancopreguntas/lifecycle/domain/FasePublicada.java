package co.unicauca.bancopreguntas.lifecycle.domain;

import java.util.Set;

/** Publicada: ya hace parte del banco; solo se puede archivar. */
public final class FasePublicada implements FaseDePregunta {

    @Override
    public EstadoPregunta estado() {
        return EstadoPregunta.PUBLICADA;
    }

    @Override
    public Set<EstadoPregunta> destinos() {
        return Set.of();
    }
}
