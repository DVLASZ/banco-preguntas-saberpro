package co.unicauca.bancopreguntas.question.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La consulta solo incluye los filtros que se pidieron. El patrón «(:filtro IS NULL OR columna = :filtro)» en una
 * sola consulta fija obliga a PostgreSQL a un plan genérico que no usa los índices (recorre toda la tabla en cada
 * búsqueda); armada a medida, usa el índice del autor y el del orden. El conteo se omite cuando la página no está
 * llena y ya se conoce el total.
 */
class QuestionJpaRepositoryCustomImpl implements QuestionJpaRepositoryCustom {

    private static final String TEXTO = "unaccent(lower(q.nombre || ' ' || q.enunciado || ' ' || q.tema || ' ' || q.subtema))"
            + " LIKE unaccent(lower(:texto))";

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public Page<QuestionEntity> buscar(String autor, String estado, String competencia, String texto, Pageable pageable) {
        List<String> condiciones = new ArrayList<>();
        Map<String, Object> parametros = new LinkedHashMap<>();
        if (autor != null) {
            condiciones.add("q.author_id = :autor");
            parametros.put("autor", autor);
        }
        if (estado != null) {
            condiciones.add("q.estado = :estado");
            parametros.put("estado", estado);
        }
        if (competencia != null) {
            condiciones.add("q.competencia = :competencia");
            parametros.put("competencia", competencia);
        }
        if (texto != null) {
            condiciones.add(TEXTO);
            parametros.put("texto", texto);
        }
        String donde = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);

        Query consulta = em.createNativeQuery("SELECT * FROM question q" + donde + " ORDER BY q.creada_en DESC, q.id",
                QuestionEntity.class);
        parametros.forEach(consulta::setParameter);
        consulta.setFirstResult((int) pageable.getOffset());
        consulta.setMaxResults(pageable.getPageSize());
        List<QuestionEntity> contenido = consulta.getResultList();

        return PageableExecutionUtils.getPage(contenido, pageable, () -> {
            Query conteo = em.createNativeQuery("SELECT count(*) FROM question q" + donde);
            parametros.forEach(conteo::setParameter);
            return ((Number) conteo.getSingleResult()).longValue();
        });
    }
}
