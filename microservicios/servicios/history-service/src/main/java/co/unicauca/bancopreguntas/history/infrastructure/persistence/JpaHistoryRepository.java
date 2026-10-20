package co.unicauca.bancopreguntas.history.infrastructure.persistence;

import co.unicauca.bancopreguntas.history.application.HistoryApplicationService;
import co.unicauca.bancopreguntas.history.application.port.HistoryRepository;
import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.history.domain.Observador;
import co.unicauca.bancopreguntas.history.domain.Pagina;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Adaptador de persistencia del puerto {@link HistoryRepository}. */
@Component
public class JpaHistoryRepository implements HistoryRepository {

    private final HistoryEntryJpaRepository repositorio;

    public JpaHistoryRepository(HistoryEntryJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(EntradaDeHistorial entrada) {
        repositorio.saveAndFlush(HistoryEntryEntity.de(entrada));
    }

    @Override
    public List<EntradaDeHistorial> lineaDeTiempo(String preguntaId, Set<TipoEntrada> tipos) {
        List<HistoryEntryEntity> filas = tipos == null
                ? repositorio.findByQuestionIdOrderByOccurredAtAscIdAsc(preguntaId)
                : repositorio.findByQuestionIdAndTypeInOrderByOccurredAtAscIdAsc(preguntaId,
                        tipos.stream().map(Enum::name).toList());
        return filas.stream().map(HistoryEntryEntity::aDominio).toList();
    }

    @Override
    public Pagina<EntradaDeHistorial> visiblesPara(Observador o, int pagina, int tamano) {
        List<String> tiposDelAutor = HistoryApplicationService.tiposVisiblesParaElAutor().stream().map(Enum::name).toList();
        Page<HistoryEntryEntity> resultado = repositorio.visiblesPara(o.administrador(), o.autor(), o.revisor(),
                o.usuarioId(), tiposDelAutor, PageRequest.of(pagina - 1, tamano));
        return aPagina(resultado, pagina, tamano);
    }

    @Override
    public Pagina<EntradaDeHistorial> auditoria(FiltroAuditoria filtro, int pagina, int tamano) {
        Specification<HistoryEntryEntity> especificacion = (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.tipo() != null) {
                condiciones.add(cb.equal(raiz.get("type"), filtro.tipo().name()));
            }
            if (filtro.actorId() != null) {
                condiciones.add(cb.equal(raiz.get("actorId"), filtro.actorId()));
            }
            if (filtro.preguntaId() != null) {
                condiciones.add(cb.equal(raiz.get("questionId"), filtro.preguntaId()));
            }
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(raiz.get("occurredAt"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThanOrEqualTo(raiz.get("occurredAt"), filtro.hasta()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
        Sort orden = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.asc("id"));
        return aPagina(repositorio.findAll(especificacion, PageRequest.of(pagina - 1, tamano, orden)), pagina, tamano);
    }

    private static Pagina<EntradaDeHistorial> aPagina(Page<HistoryEntryEntity> resultado, int pagina, int tamano) {
        return new Pagina<>(resultado.getContent().stream().map(HistoryEntryEntity::aDominio).toList(), pagina, tamano,
                (int) resultado.getTotalElements());
    }
}
