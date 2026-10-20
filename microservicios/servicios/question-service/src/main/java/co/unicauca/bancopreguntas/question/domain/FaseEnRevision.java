package co.unicauca.bancopreguntas.question.domain;

import java.util.Set;

/** En revisión: los revisores la evalúan; termina aprobada (unanimidad) o rechazada (un solo rechazo). */
public final class FaseEnRevision implements FaseDePregunta {

    @Override
    public EstadoPregunta estado() {
        return EstadoPregunta.EN_REVISION;
    }

    @Override
    public Set<EstadoPregunta> destinos() {
        return Set.of(EstadoPregunta.APROBADA, EstadoPregunta.RECHAZADA);
    }

    @Override
    public EstadoPregunta aprobar() {
        return EstadoPregunta.APROBADA;
    }

    @Override
    public EstadoPregunta rechazar() {
        return EstadoPregunta.RECHAZADA;
    }
}
