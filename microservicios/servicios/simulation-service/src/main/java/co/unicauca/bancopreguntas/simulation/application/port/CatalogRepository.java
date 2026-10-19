package co.unicauca.bancopreguntas.simulation.application.port;

import co.unicauca.bancopreguntas.simulation.domain.PreguntaDeCatalogo;
import co.unicauca.bancopreguntas.simulation.domain.ResumenDelCatalogo;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: el catálogo de preguntas publicadas que alimentan los simulacros. */
public interface CatalogRepository {

    /** Guarda la pregunta, reemplazando la copia anterior de la misma pregunta. */
    void guardar(PreguntaDeCatalogo pregunta);

    Optional<PreguntaDeCatalogo> buscar(String preguntaId);

    /** Las preguntas disponibles (no archivadas). */
    List<PreguntaDeCatalogo> disponibles();

    ResumenDelCatalogo resumen();
}
