package co.unicauca.bancopreguntas.simulation.application;

import co.unicauca.bancopreguntas.simulation.application.port.CatalogRepository;
import co.unicauca.bancopreguntas.simulation.domain.PreguntaDeCatalogo;
import co.unicauca.bancopreguntas.simulation.domain.ResumenDelCatalogo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Doble de prueba del catálogo, en memoria. */
class InMemoryCatalogRepository implements CatalogRepository {

    private final Map<String, PreguntaDeCatalogo> preguntas = new LinkedHashMap<>();

    @Override
    public void guardar(PreguntaDeCatalogo pregunta) {
        preguntas.put(pregunta.preguntaId(), pregunta);
    }

    @Override
    public Optional<PreguntaDeCatalogo> buscar(String preguntaId) {
        return Optional.ofNullable(preguntas.get(preguntaId));
    }

    @Override
    public List<PreguntaDeCatalogo> disponibles() {
        return preguntas.values().stream().filter(PreguntaDeCatalogo::disponible).toList();
    }

    @Override
    public ResumenDelCatalogo resumen() {
        Map<String, Integer> competencias = new TreeMap<>();
        Map<String, Integer> dificultades = new TreeMap<>();
        disponibles().forEach(p -> {
            competencias.merge(p.competencia(), 1, Integer::sum);
            dificultades.merge(p.dificultad(), 1, Integer::sum);
        });
        return new ResumenDelCatalogo(disponibles().size(), competencias, dificultades);
    }
}
