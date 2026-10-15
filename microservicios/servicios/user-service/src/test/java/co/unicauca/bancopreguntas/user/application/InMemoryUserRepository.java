package co.unicauca.bancopreguntas.user.application;

import co.unicauca.bancopreguntas.user.application.port.UserRepository;
import co.unicauca.bancopreguntas.user.domain.FiltroUsuarios;
import co.unicauca.bancopreguntas.user.domain.Pagina;
import co.unicauca.bancopreguntas.user.domain.Usuario;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba del repositorio de usuarios, en memoria. */
class InMemoryUserRepository implements UserRepository {

    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();

    @Override
    public void guardar(Usuario usuario) {
        usuarios.put(usuario.id(), usuario);
    }

    @Override
    public Optional<Usuario> buscar(String id) {
        return Optional.ofNullable(usuarios.get(id));
    }

    @Override
    public List<Usuario> todos() {
        return List.copyOf(usuarios.values());
    }

    @Override
    public Pagina<Usuario> listar(FiltroUsuarios f, int pagina, int tamano) {
        List<Usuario> filtrados = usuarios.values().stream()
                .filter(u -> !f.soloActivos() || u.activo())
                .filter(u -> f.rol() == null || u.tieneRol(f.rol()))
                .filter(u -> f.texto() == null || u.id().toLowerCase().contains(f.texto().toLowerCase())
                        || u.nombre().toLowerCase().contains(f.texto().toLowerCase()))
                .sorted(Comparator.comparing(Usuario::nombre))
                .toList();
        int desde = Math.min((pagina - 1) * tamano, filtrados.size());
        int hasta = Math.min(desde + tamano, filtrados.size());
        return new Pagina<>(filtrados.subList(desde, hasta), pagina, tamano, filtrados.size());
    }

    @Override
    public long contar() {
        return usuarios.size();
    }
}
