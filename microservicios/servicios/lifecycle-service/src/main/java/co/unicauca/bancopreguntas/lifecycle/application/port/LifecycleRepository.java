package co.unicauca.bancopreguntas.lifecycle.application.port;

import co.unicauca.bancopreguntas.lifecycle.domain.CambioEstado;
import co.unicauca.bancopreguntas.lifecycle.domain.CicloDeVida;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: dónde se guarda el ciclo de vida de las preguntas y su historial de estados. */
public interface LifecycleRepository {

    /** Guarda el ciclo de vida, reemplazando el anterior de la misma pregunta. */
    void guardar(CicloDeVida ciclo);

    Optional<CicloDeVida> buscar(String preguntaId);

    void registrarCambioEstado(String preguntaId, CambioEstado cambio);

    /** El historial de estados de la pregunta, del más antiguo al más reciente. */
    List<CambioEstado> historial(String preguntaId);
}
