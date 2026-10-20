package co.unicauca.bancopreguntas.simulation.infrastructure.persistence;

import co.unicauca.bancopreguntas.simulation.application.port.CatalogRepository;
import co.unicauca.bancopreguntas.simulation.domain.PreguntaDeCatalogo;
import co.unicauca.bancopreguntas.simulation.domain.ResumenDelCatalogo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Adaptador de persistencia del puerto {@link CatalogRepository} (patrón Adapter). */
@Component
public class JpaCatalogRepository implements CatalogRepository {

    private final CatalogJpaRepository repositorio;

    public JpaCatalogRepository(CatalogJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(PreguntaDeCatalogo p) {
        CatalogQuestionEntity e = repositorio.findById(p.preguntaId()).orElseGet(CatalogQuestionEntity::new);
        e.questionId = p.preguntaId();
        e.version = p.version();
        e.title = p.titulo();
        e.competency = p.competencia();
        e.topic = p.tema();
        e.subtopic = p.subtema();
        e.difficulty = p.dificultad();
        e.context = p.contexto();
        e.statement = p.enunciado();
        e.optionA = p.opcionA();
        e.optionB = p.opcionB();
        e.optionC = p.opcionC();
        e.optionD = p.opcionD();
        e.correctAnswer = String.valueOf(p.respuestaCorrecta());
        e.publishedAt = p.publicadaEn();
        e.available = p.disponible();
        repositorio.saveAndFlush(e);
    }

    @Override
    public Optional<PreguntaDeCatalogo> buscar(String preguntaId) {
        return repositorio.findById(preguntaId).map(JpaCatalogRepository::aDominio);
    }

    @Override
    public List<PreguntaDeCatalogo> disponibles() {
        return repositorio.findByAvailableTrue().stream().map(JpaCatalogRepository::aDominio).toList();
    }

    @Override
    public ResumenDelCatalogo resumen() {
        Map<String, Integer> porCompetencia = contar(repositorio.contarPorCompetencia());
        Map<String, Integer> porDificultad = contar(repositorio.contarPorDificultad());
        return new ResumenDelCatalogo(porCompetencia.values().stream().mapToInt(Integer::intValue).sum(),
                porCompetencia, porDificultad);
    }

    private static Map<String, Integer> contar(List<Object[]> filas) {
        Map<String, Integer> conteo = new TreeMap<>();
        filas.forEach(f -> conteo.put((String) f[0], ((Number) f[1]).intValue()));
        return conteo;
    }

    static PreguntaDeCatalogo aDominio(CatalogQuestionEntity e) {
        return new PreguntaDeCatalogo(e.questionId, e.version, e.title, e.competency, e.topic, e.subtopic, e.difficulty,
                e.context, e.statement, e.optionA, e.optionB, e.optionC, e.optionD, e.correctAnswer.charAt(0),
                e.publishedAt, e.available);
    }
}
