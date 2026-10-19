package co.unicauca.bancopreguntas.tracking.application.port;

import co.unicauca.bancopreguntas.tracking.domain.Pagina;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoRegistrado;

import java.util.List;

/** Puerto de salida: el historial de simulacros terminados de los estudiantes. */
public interface HistorialRepository {

    /** Guarda el resultado; si ya existe uno de ese simulacro, no hace nada. */
    void guardar(ResultadoRegistrado resultado);

    /** El historial de un estudiante, el más reciente primero. {@code pagina} cuenta desde 1. */
    Pagina<ResultadoRegistrado> delEstudiante(String estudianteId, int pagina, int tamano);

    /** Todos los resultados de un estudiante. */
    List<ResultadoRegistrado> todosDelEstudiante(String estudianteId);

    /** Todos los resultados de todos los estudiantes (para el reporte de grupo). */
    List<ResultadoRegistrado> todos();
}
