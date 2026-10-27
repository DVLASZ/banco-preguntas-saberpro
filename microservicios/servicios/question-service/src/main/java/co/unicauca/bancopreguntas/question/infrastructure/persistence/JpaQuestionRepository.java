package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import co.unicauca.bancopreguntas.question.application.port.QuestionRepository;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.Pagina;
import co.unicauca.bancopreguntas.question.domain.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia: implementa el puerto {@link QuestionRepository} con Spring Data JPA
 * sobre PostgreSQL (patrón Adapter).
 */
@Repository
public class JpaQuestionRepository implements QuestionRepository {

    private final QuestionJpaRepository preguntas;

    public JpaQuestionRepository(QuestionJpaRepository preguntas) {
        this.preguntas = preguntas;
    }

    @Override
    public Question guardar(Question pregunta) {
        QuestionEntity entidad = preguntas.findById(pregunta.getId()).orElseGet(QuestionEntity::new);
        QuestionMapper.aEntidad(pregunta, entidad);
        return QuestionMapper.aDominio(preguntas.saveAndFlush(entidad));
    }

    @Override
    public Optional<Question> buscarPorId(String id) {
        return preguntas.findById(id).map(QuestionMapper::aDominio);
    }

    @Override
    public Pagina<Question> buscar(String autor, FiltroPreguntas filtro, int pagina, int tamano) {
        Page<QuestionEntity> resultado = preguntas.buscar(
                autor,
                filtro.estado() == null ? null : filtro.estado().name(),
                filtro.competencia() == null ? null : filtro.competencia().name(),
                patronDeTexto(filtro.texto()),
                PageRequest.of(pagina - 1, tamano));
        List<Question> elementos = resultado.getContent().stream().map(QuestionMapper::aDominio).toList();
        return new Pagina<>(elementos, pagina, tamano, Math.toIntExact(resultado.getTotalElements()));
    }

    /** {@code %texto%} con los comodines de LIKE escapados; {@code null} si no hay texto que buscar. */
    private static String patronDeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String escapado = texto.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
