package co.unicauca.bancopreguntas.question.domain;

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
