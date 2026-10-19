package co.unicauca.bancopreguntas.simulation.application.port;

import co.unicauca.bancopreguntas.simulation.domain.Pagina;
import co.unicauca.bancopreguntas.simulation.domain.Simulacro;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: persistencia de los simulacros. */
public interface SimulacroRepository {

    /** Crea el simulacro o actualiza el existente (respuestas, estado y calificación). */
    void guardar(Simulacro simulacro);

    Optional<Simulacro> buscar(String id);

    /** Los simulacros de un estudiante, el más reciente primero. {@code pagina} cuenta desde 1. */
    Pagina<Simulacro> delEstudiante(String estudianteId, int pagina, int tamano);

    /** Simulacros en curso cuyo tiempo se agotó antes de {@code ahora}, para cerrarlos. */
    List<Simulacro> vencidos(Instant ahora, int limite);
}
