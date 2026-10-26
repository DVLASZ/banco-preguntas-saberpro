package co.unicauca.bancopreguntas.history.application;

import co.unicauca.bancopreguntas.history.application.port.HistoryRepository;
import co.unicauca.bancopreguntas.history.application.port.QuestionRegistry;
import co.unicauca.bancopreguntas.history.domain.EntradaDeHistorial;
import co.unicauca.bancopreguntas.history.domain.Observador;
import co.unicauca.bancopreguntas.history.domain.Pagina;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Doble de prueba del repositorio del historial: aplica en memoria las mismas reglas de visibilidad. */
class InMemoryHistoryRepository implements HistoryRepository {

    private final List<EntradaDeHistorial> entradas = new ArrayList<>();
    private final QuestionRegistry preguntas;

    InMemoryHistoryRepository(QuestionRegistry preguntas) {
        this.preguntas = preguntas;
    }

    List<EntradaDeHistorial> todas() {
        return List.copyOf(entradas);
    }

    @Override
    public void guardar(EntradaDeHistorial entrada) {
        entradas.add(entrada);
    }

    @Override
    public List<EntradaDeHistorial> lineaDeTiempo(String preguntaId, Set<TipoEntrada> tipos) {
        return entradas.stream()
                .filter(e -> preguntaId.equals(e.preguntaId()))
                .filter(e -> tipos == null || tipos.contains(e.tipo()))
                .sorted(Comparator.comparing(EntradaDeHistorial::ocurridaEn))
                .toList();
    }

    @Override
    public Pagina<EntradaDeHistorial> visiblesPara(Observador o, int pagina, int tamano) {
        Predicate<EntradaDeHistorial> visible = e -> o.administrador()
                || (e.preguntaId() != null
                && ((o.autor() && e.tipo().visibleParaElAutor() && preguntas.buscar(e.preguntaId())
                        .map(i -> i.autorId().equals(o.usuarioId())).orElse(false))
                || (o.revisor() && preguntas.esRevisor(e.preguntaId(), o.usuarioId()))));
        return paginar(entradas.stream().filter(visible)
                .sorted(Comparator.comparing(EntradaDeHistorial::ocurridaEn).reversed()).toList(), pagina, tamano);
    }

    private static Pagina<EntradaDeHistorial> paginar(List<EntradaDeHistorial> lista, int pagina, int tamano) {
        int desde = Math.min((pagina - 1) * tamano, lista.size());
        int hasta = Math.min(desde + tamano, lista.size());
        return new Pagina<>(lista.subList(desde, hasta), pagina, tamano, lista.size());
    }
}
