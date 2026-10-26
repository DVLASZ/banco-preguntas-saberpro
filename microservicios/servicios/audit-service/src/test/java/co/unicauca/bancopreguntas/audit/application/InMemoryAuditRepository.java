package co.unicauca.bancopreguntas.audit.application;

import co.unicauca.bancopreguntas.audit.application.port.AuditRepository;
import co.unicauca.bancopreguntas.audit.domain.EntradaDeAuditoria;
import co.unicauca.bancopreguntas.audit.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.audit.domain.Pagina;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Doble de prueba de la auditoría, en memoria. */
class InMemoryAuditRepository implements AuditRepository {

    private final List<EntradaDeAuditoria> todas = new ArrayList<>();

    @Override
    public void guardar(EntradaDeAuditoria entrada) {
        todas.add(entrada);
    }

    @Override
    public Pagina<EntradaDeAuditoria> consultar(FiltroAuditoria f, int pagina, int tamano) {
        List<EntradaDeAuditoria> lista = todas.stream()
                .filter(e -> f.tipo() == null || e.tipo() == f.tipo())
                .filter(e -> f.actorId() == null || f.actorId().equals(e.actorId()))
                .filter(e -> f.preguntaId() == null || f.preguntaId().equals(e.preguntaId()))
                .filter(e -> f.desde() == null || !e.ocurridaEn().isBefore(f.desde()))
                .filter(e -> f.hasta() == null || !e.ocurridaEn().isAfter(f.hasta()))
                .sorted(Comparator.comparing(EntradaDeAuditoria::ocurridaEn).reversed()).toList();
        int desde = Math.min((pagina - 1) * tamano, lista.size());
        int hasta = Math.min(desde + tamano, lista.size());
        return new Pagina<>(lista.subList(desde, hasta), pagina, tamano, lista.size());
    }

    List<EntradaDeAuditoria> todas() {
        return todas;
    }
}
