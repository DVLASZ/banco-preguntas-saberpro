package co.unicauca.bancopreguntas.history.infrastructure.persistence;

import co.unicauca.bancopreguntas.history.application.HistoryApplicationService;
import co.unicauca.bancopreguntas.history.application.port.HistoryRepository;
import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.Observador;
import co.unicauca.bancopreguntas.history.domain.Pagina;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

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

    private static Pagina<EntradaDeHistorial> aPagina(Page<HistoryEntryEntity> resultado, int pagina, int tamano) {
        return new Pagina<>(resultado.getContent().stream().map(HistoryEntryEntity::aDominio).toList(), pagina, tamano,
                (int) resultado.getTotalElements());
    }
}
