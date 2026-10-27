package co.unicauca.bancopreguntas.lifecycle.domain;

import java.util.Set;

/** Pendiente de revisión: espera que el administrador le asigne revisores. */
public final class FasePendienteDeRevision implements FaseDePregunta {

    @Override
    public EstadoPregunta estado() {
        return EstadoPregunta.PENDIENTE_REVISION;
    }

    @Override
    public Set<EstadoPregunta> destinos() {
        return Set.of(EstadoPregunta.EN_REVISION);
    }

    @Override
    public EstadoPregunta iniciarRevision() {
        return EstadoPregunta.EN_REVISION;
    }
}
