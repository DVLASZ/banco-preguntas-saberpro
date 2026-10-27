package co.unicauca.bancopreguntas.lifecycle.domain;

import java.util.Set;

/** Borrador: el autor la redacta y la puede modificar; el único paso adelante es enviarla a revisión. */
public final class FaseBorrador implements FaseDePregunta {

    @Override
    public EstadoPregunta estado() {
        return EstadoPregunta.BORRADOR;
    }

    @Override
    public Set<EstadoPregunta> destinos() {
        return Set.of(EstadoPregunta.PENDIENTE_REVISION);
    }

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public EstadoPregunta enviarARevision() {
        return EstadoPregunta.PENDIENTE_REVISION;
    }
}
