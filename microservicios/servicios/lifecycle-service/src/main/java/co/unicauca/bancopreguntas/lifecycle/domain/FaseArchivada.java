package co.unicauca.bancopreguntas.lifecycle.domain;

import java.util.Set;

/** Archivada: fuera de circulación; no admite ningún cambio de fase. */
public final class FaseArchivada implements FaseDePregunta {

    @Override
    public EstadoPregunta estado() {
        return EstadoPregunta.ARCHIVADA;
    }

    @Override
    public Set<EstadoPregunta> destinos() {
        return Set.of();
    }
}
