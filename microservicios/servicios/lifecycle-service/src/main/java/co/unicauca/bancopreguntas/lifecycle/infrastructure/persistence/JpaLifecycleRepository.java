package co.unicauca.bancopreguntas.lifecycle.infrastructure.persistence;

import co.unicauca.bancopreguntas.lifecycle.application.port.LifecycleRepository;
import co.unicauca.bancopreguntas.lifecycle.domain.CambioEstado;
import co.unicauca.bancopreguntas.lifecycle.domain.CicloDeVida;
import co.unicauca.bancopreguntas.lifecycle.domain.EstadoPregunta;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adaptador de persistencia del puerto {@link LifecycleRepository} con Spring Data JPA (patrón Adapter). */
@Repository
public class JpaLifecycleRepository implements LifecycleRepository {

    private final LifecycleJpaRepository ciclos;
    private final StateHistoryJpaRepository historial;

    public JpaLifecycleRepository(LifecycleJpaRepository ciclos, StateHistoryJpaRepository historial) {
        this.ciclos = ciclos;
        this.historial = historial;
    }

    @Override
    public void guardar(CicloDeVida c) {
        LifecycleEntity e = ciclos.findById(c.getPreguntaId()).orElseGet(LifecycleEntity::new);
        e.preguntaId = c.getPreguntaId();
        e.autorId = c.getAutorId();
        e.estado = c.getEstado().name();
        e.version = c.getVersion();
        e.sequence = c.getSecuencia();
        e.actualizadoEn = c.getActualizadoEn();
        ciclos.saveAndFlush(e);
    }

    @Override
    public Optional<CicloDeVida> buscar(String preguntaId) {
        return ciclos.findById(preguntaId).map(e -> new CicloDeVida(e.preguntaId, e.autorId,
                EstadoPregunta.valueOf(e.estado), e.version, e.sequence, e.actualizadoEn));
    }

    @Override
    public void registrarCambioEstado(String preguntaId, CambioEstado cambio) {
        EstadoPregunta desde = cambio.desde();
        historial.save(new StateHistoryEntity(preguntaId, desde == null ? null : desde.name(), cambio.hacia().name(),
                cambio.cambiadoPor(), cambio.fecha()));
    }

    @Override
    public List<CambioEstado> historial(String preguntaId) {
        return historial.findByQuestionIdOrderByFechaAscIdAsc(preguntaId).stream()
                .map(h -> new CambioEstado(h.getDesde() == null ? null : EstadoPregunta.valueOf(h.getDesde()),
                        EstadoPregunta.valueOf(h.getHacia()), h.getCambiadoPor(), h.getFecha()))
                .toList();
    }
}
