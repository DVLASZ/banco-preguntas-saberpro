package co.unicauca.bancopreguntas.user.infrastructure.persistence;

import co.unicauca.bancopreguntas.user.application.port.UserRepository;
import co.unicauca.bancopreguntas.user.domain.FiltroUsuarios;
import co.unicauca.bancopreguntas.user.domain.Pagina;
import co.unicauca.bancopreguntas.user.domain.Usuario;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Adaptador de persistencia del puerto {@link UserRepository}. */
@Component
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository repositorio;

    public JpaUserRepository(UserJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardar(Usuario usuario) {
        UserEntity entidad = repositorio.findById(usuario.id()).orElseGet(() -> UserEntity.de(usuario));
        entidad.copiar(usuario);
        repositorio.saveAndFlush(entidad);
    }

    @Override
    public Optional<Usuario> buscar(String id) {
        return repositorio.findById(id).map(UserEntity::aDominio);
    }

    @Override
    public List<Usuario> todos() {
        return repositorio.findAll().stream().map(UserEntity::aDominio).toList();
    }

    @Override
    public Pagina<Usuario> listar(FiltroUsuarios filtro, int pagina, int tamano) {
        Specification<UserEntity> especificacion = (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.soloActivos()) {
                condiciones.add(cb.isTrue(raiz.get("active")));
            }
            if (filtro.rol() != null) {
                condiciones.add(cb.like(raiz.get("roles"), "%|" + filtro.rol() + "|%"));
            }
            if (filtro.texto() != null) {
                String patron = "%" + escapar(filtro.texto().toLowerCase(Locale.ROOT)) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(raiz.get("username")), patron, '\\'),
                        cb.like(cb.lower(raiz.get("fullName")), patron, '\\'),
                        cb.like(cb.lower(cb.coalesce(raiz.get("email"), "")), patron, '\\')));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
        Page<UserEntity> resultado = repositorio.findAll(especificacion,
                PageRequest.of(pagina - 1, tamano, Sort.by("fullName").ascending().and(Sort.by("username"))));
        return new Pagina<>(resultado.getContent().stream().map(UserEntity::aDominio).toList(), pagina, tamano,
                (int) resultado.getTotalElements());
    }

    @Override
    public long contar() {
        return repositorio.count();
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
