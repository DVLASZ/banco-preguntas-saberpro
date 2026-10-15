package co.unicauca.bancopreguntas.user.application.port;

import co.unicauca.bancopreguntas.user.domain.FiltroUsuarios;
import co.unicauca.bancopreguntas.user.domain.Pagina;
import co.unicauca.bancopreguntas.user.domain.Usuario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: persistencia de los usuarios. */
public interface UserRepository {

    /** Crea o actualiza al usuario. */
    void guardar(Usuario usuario);

    Optional<Usuario> buscar(String id);

    /** Todos los usuarios conocidos, activos o no. */
    List<Usuario> todos();

    Pagina<Usuario> listar(FiltroUsuarios filtro, int pagina, int tamano);

    /** Cuántos usuarios hay, activos o no. */
    long contar();
}
