package co.unicauca.bancopreguntas.email.application;

import co.unicauca.bancopreguntas.email.application.port.SentEmailRepository;
import co.unicauca.bancopreguntas.email.domain.CorreoEnviado;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;
import co.unicauca.bancopreguntas.email.domain.Pagina;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Doble de prueba del registro de correos, en memoria. */
class InMemorySentEmailRepository implements SentEmailRepository {

    private final List<CorreoEnviado> todos = new ArrayList<>();

    @Override
    public void guardar(CorreoEnviado correo) {
        todos.add(correo);
    }

    @Override
    public Pagina<CorreoEnviado> listar(EstadoCorreo estado, int pagina, int tamano) {
        List<CorreoEnviado> lista = todos.stream().filter(c -> estado == null || c.estado() == estado)
                .sorted(Comparator.comparing(CorreoEnviado::intentadoEn).reversed()).toList();
        int desde = Math.min((pagina - 1) * tamano, lista.size());
        int hasta = Math.min(desde + tamano, lista.size());
        return new Pagina<>(lista.subList(desde, hasta), pagina, tamano, lista.size());
    }

    List<CorreoEnviado> todos() {
        return todos;
    }
}
