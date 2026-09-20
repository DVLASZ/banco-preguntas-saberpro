package co.unicauca.saberpro.api.repository;

import co.unicauca.saberpro.api.model.QuestionMapper;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Adaptador que implementa el puerto {@link QuestionRepository} del dominio
 * usando Spring Data JPA: asi {@code QuestionService} funciona igual que con
 * el repositorio en memoria del monolito, sin saber que hay una base de datos.
 */
@Repository
public class QuestionJpaAdapter implements QuestionRepository {

    private static final String PREFIJO_ID = "P-";

    private final QuestionJpaRepository jpa;
    private int ultimoIdEmitido = 0;

    public QuestionJpaAdapter(QuestionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Question> obtenerTodas() {
        return jpa.findAll(Sort.by("id")).stream().map(QuestionMapper::toDomain).toList();
    }

    @Override
    public Question obtenerPorId(String id) {
        return jpa.findById(id).map(QuestionMapper::toDomain).orElse(null);
    }

    @Override
    public void actualizar(Question pregunta) {
        if (!jpa.existsById(pregunta.getId())) {
            throw new IllegalArgumentException("No existe una pregunta con id " + pregunta.getId());
        }
        jpa.save(QuestionMapper.toEntity(pregunta));
    }

    @Override
    public void crear(Question pregunta) {
        if (jpa.existsById(pregunta.getId())) {
            throw new IllegalArgumentException("Ya existe una pregunta con id " + pregunta.getId());
        }
        jpa.save(QuestionMapper.toEntity(pregunta));
    }

    @Override
    public synchronized String generarNuevoId() {
        int maximoEnBaseDeDatos = jpa.findAllIds().stream()
                .mapToInt(QuestionJpaAdapter::consecutivoDe)
                .max()
                .orElse(0);
        ultimoIdEmitido = Math.max(maximoEnBaseDeDatos, ultimoIdEmitido) + 1;
        return String.format("%s%03d", PREFIJO_ID, ultimoIdEmitido);
    }

    private static int consecutivoDe(String id) {
        if (!id.startsWith(PREFIJO_ID)) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(PREFIJO_ID.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
