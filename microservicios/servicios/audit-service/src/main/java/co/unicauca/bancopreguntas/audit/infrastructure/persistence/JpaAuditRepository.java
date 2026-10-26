package co.unicauca.bancopreguntas.audit.infrastructure.persistence;

import co.unicauca.bancopreguntas.audit.application.port.AuditRepository;
import co.unicauca.bancopreguntas.audit.domain.EntradaDeAuditoria;
import co.unicauca.bancopreguntas.audit.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.audit.domain.Pagina;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Adaptador de persistencia del puerto {@link AuditRepository}. */
@Component
public class JpaAuditRepository implements AuditRepository {

    private final AuditEntryJpaRepository repositorio;

    public JpaAuditRepository(AuditEntryJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(EntradaDeAuditoria entrada) {
        repositorio.saveAndFlush(AuditEntryEntity.de(entrada));
    }

    @Override
    public Pagina<EntradaDeAuditoria> consultar(FiltroAuditoria filtro, int pagina, int tamano) {
        Specification<AuditEntryEntity> especificacion = (raiz, consulta, cb) -> {
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
        Page<AuditEntryEntity> resultado = repositorio.findAll(especificacion, PageRequest.of(pagina - 1, tamano, orden));
        return new Pagina<>(resultado.getContent().stream().map(AuditEntryEntity::aDominio).toList(), pagina, tamano,
                (int) resultado.getTotalElements());
    }
}
