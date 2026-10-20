package co.unicauca.bancopreguntas.simulation.infrastructure.persistence;

import co.unicauca.bancopreguntas.simulation.application.port.SimulacroRepository;
import co.unicauca.bancopreguntas.simulation.domain.CriteriosDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.EstadoSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.ItemDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.Pagina;
import co.unicauca.bancopreguntas.simulation.domain.PreguntaDeCatalogo;
import co.unicauca.bancopreguntas.simulation.domain.ResultadoDeSimulacro;
import co.unicauca.bancopreguntas.simulation.domain.ResultadoPorCompetencia;
import co.unicauca.bancopreguntas.simulation.domain.Simulacro;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Adaptador de persistencia del puerto {@link SimulacroRepository} (patrón Adapter). */
@Component
public class JpaSimulacroRepository implements SimulacroRepository {

    private static final TypeReference<List<ResultadoPorCompetencia>> POR_COMPETENCIA = new TypeReference<>() { };

    private final SimulationJpaRepository repositorio;
    private final ObjectMapper json;

    public JpaSimulacroRepository(SimulationJpaRepository repositorio, ObjectMapper json) {
        this.repositorio = repositorio;
        this.json = json;
    }

    @Override
    public void guardar(Simulacro s) {
        SimulationEntity e = repositorio.findById(s.getId()).orElse(null);
        if (e == null) {
            e = nueva(s);
        }
        e.status = s.getEstado().name();
        e.finishedAt = s.getFinalizadoEn();
        ResultadoDeSimulacro r = s.getResultado();
        if (r != null) {
            e.total = r.total();
            e.correct = r.correctas();
            e.incorrect = r.incorrectas();
            e.unanswered = r.sinResponder();
            e.score = BigDecimal.valueOf(r.puntaje()).setScale(2, RoundingMode.HALF_UP);
            e.byCompetency = aJson(r.porCompetencia());
        }
        for (ItemDeSimulacro item : s.getItems()) {
            SimulationItemEntity fila = e.items.get(item.orden() - 1);
            fila.answer = item.respuesta() == null ? null : String.valueOf(item.respuesta());
        }
        repositorio.saveAndFlush(e);
    }

    @Override
    public Optional<Simulacro> buscar(String id) {
        return repositorio.findById(id).map(this::aDominio);
    }

    @Override
    public Pagina<Simulacro> delEstudiante(String estudianteId, int pagina, int tamano) {
        Page<SimulationEntity> resultado = repositorio.findByStudentIdOrderByStartedAtDesc(estudianteId,
                PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent().stream().map(this::aDominio).toList(), pagina, tamano,
                resultado.getTotalElements());
    }

    @Override
    public List<Simulacro> vencidos(Instant ahora, int limite) {
        return repositorio.findByStatusAndDueAtBefore(EstadoSimulacro.EN_CURSO.name(), ahora, PageRequest.of(0, limite))
                .stream().map(this::aDominio).toList();
    }

    // ------------------------------------------------------------------ mapeo

    private static SimulationEntity nueva(Simulacro s) {
        SimulationEntity e = new SimulationEntity();
        e.id = s.getId();
        e.studentId = s.getEstudianteId();
        e.startedAt = s.getIniciadoEn();
        e.dueAt = s.getVenceEn();
        CriteriosDeSimulacro c = s.getCriterios();
        e.durationMinutes = c.duracionMinutos();
        e.requestedCount = c.cantidad();
        e.competencies = entreBarras(c.competencias());
        e.topics = entreBarras(c.temas());
        e.difficulties = entreBarras(c.dificultades());
        for (ItemDeSimulacro item : s.getItems()) {
            PreguntaDeCatalogo p = item.pregunta();
            SimulationItemEntity fila = new SimulationItemEntity();
            fila.simulation = e;
            fila.position = item.orden();
            fila.questionId = p.preguntaId();
            fila.version = p.version();
            fila.title = p.titulo();
            fila.competency = p.competencia();
            fila.topic = p.tema();
            fila.subtopic = p.subtema();
            fila.difficulty = p.dificultad();
            fila.context = p.contexto();
            fila.statement = p.enunciado();
            fila.optionA = p.opcionA();
            fila.optionB = p.opcionB();
            fila.optionC = p.opcionC();
            fila.optionD = p.opcionD();
            fila.correctAnswer = String.valueOf(p.respuestaCorrecta());
            e.items.add(fila);
        }
        return e;
    }

    private Simulacro aDominio(SimulationEntity e) {
        List<ItemDeSimulacro> items = new ArrayList<>();
        for (SimulationItemEntity f : e.items) {
            PreguntaDeCatalogo p = new PreguntaDeCatalogo(f.questionId, f.version, f.title, f.competency, f.topic,
                    f.subtopic, f.difficulty, f.context, f.statement, f.optionA, f.optionB, f.optionC, f.optionD,
                    f.correctAnswer.charAt(0), null, true);
            items.add(new ItemDeSimulacro(f.position, p, f.answer == null ? null : f.answer.charAt(0)));
        }
        CriteriosDeSimulacro criterios = new CriteriosDeSimulacro(deBarras(e.competencies), deBarras(e.topics),
                deBarras(e.difficulties), e.requestedCount, e.durationMinutes);
        ResultadoDeSimulacro resultado = e.total == null ? null : new ResultadoDeSimulacro(e.total, e.correct,
                e.incorrect, e.unanswered, e.score.doubleValue(), deJson(e.byCompetency));
        return new Simulacro(e.id, e.studentId, criterios, items, EstadoSimulacro.valueOf(e.status), e.startedAt, e.dueAt,
                e.finishedAt, resultado);
    }

    private static String entreBarras(Set<String> valores) {
        return valores.isEmpty() ? "|" : "|" + String.join("|", new java.util.TreeSet<>(valores)) + "|";
    }

    private static Set<String> deBarras(String texto) {
        Set<String> valores = new LinkedHashSet<>();
        Arrays.stream(texto.split("\\|")).filter(v -> !v.isBlank()).forEach(valores::add);
        return valores;
    }

    private String aJson(List<ResultadoPorCompetencia> detalle) {
        try {
            return json.writeValueAsString(detalle);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo guardar el resultado por competencia", ex);
        }
    }

    private List<ResultadoPorCompetencia> deJson(String texto) {
        try {
            return texto == null ? List.of() : json.readValue(texto, POR_COMPETENCIA);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("El resultado por competencia guardado no se puede leer", ex);
        }
    }
}
