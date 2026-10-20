package co.unicauca.bancopreguntas.tracking.infrastructure.persistence;

import co.unicauca.bancopreguntas.tracking.application.port.HistorialRepository;
import co.unicauca.bancopreguntas.tracking.domain.Pagina;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoPorCompetencia;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoRegistrado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Adaptador de persistencia del puerto {@link HistorialRepository} (patrón Adapter). */
@Component
public class JpaHistorialRepository implements HistorialRepository {

    private static final TypeReference<List<ResultadoPorCompetencia>> POR_COMPETENCIA = new TypeReference<>() { };

    private final ResultEntryJpaRepository repositorio;
    private final ObjectMapper json;

    public JpaHistorialRepository(ResultEntryJpaRepository repositorio, ObjectMapper json) {
        this.repositorio = repositorio;
        this.json = json;
    }

    @Override
    public void guardar(ResultadoRegistrado r) {
        if (repositorio.existsById(r.simulacroId())) {
            return;
        }
        ResultEntryEntity e = new ResultEntryEntity();
        e.simulationId = r.simulacroId();
        e.studentId = r.estudianteId();
        e.startedAt = r.iniciadoEn();
        e.finishedAt = r.finalizadoEn();
        e.durationSeconds = r.duracionSegundos();
        e.expired = r.expirado();
        e.total = r.total();
        e.correct = r.correctas();
        e.incorrect = r.incorrectas();
        e.unanswered = r.sinResponder();
        e.score = BigDecimal.valueOf(r.puntaje()).setScale(2, RoundingMode.HALF_UP);
        e.byCompetency = aJson(r.porCompetencia());
        repositorio.saveAndFlush(e);
    }

    @Override
    public Pagina<ResultadoRegistrado> delEstudiante(String estudianteId, int pagina, int tamano) {
        Page<ResultEntryEntity> resultado = repositorio.findByStudentIdOrderByFinishedAtDesc(estudianteId,
                PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent().stream().map(this::aDominio).toList(), pagina, tamano,
                resultado.getTotalElements());
    }

    @Override
    public List<ResultadoRegistrado> todosDelEstudiante(String estudianteId) {
        return repositorio.findByStudentId(estudianteId).stream().map(this::aDominio).toList();
    }

    @Override
    public List<ResultadoRegistrado> todos() {
        return repositorio.findAll().stream().map(this::aDominio).toList();
    }

    private ResultadoRegistrado aDominio(ResultEntryEntity e) {
        return new ResultadoRegistrado(e.simulationId, e.studentId, e.startedAt, e.finishedAt, e.durationSeconds,
                e.expired, e.total, e.correct, e.incorrect, e.unanswered, e.score.doubleValue(), deJson(e.byCompetency));
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
            return json.readValue(texto, POR_COMPETENCIA);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("El resultado por competencia guardado no se puede leer", ex);
        }
    }
}
