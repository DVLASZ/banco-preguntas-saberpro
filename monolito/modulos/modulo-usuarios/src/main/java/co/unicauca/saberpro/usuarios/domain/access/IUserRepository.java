package co.unicauca.saberpro.usuarios.domain.access;

import co.unicauca.saberpro.usuarios.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Abstraccion (contrato) que define como el resto de la aplicacion accede a
 * los usuarios persistidos, sin conocer la tecnologia de almacenamiento
 * concreta. Sigue la misma idea de {@code IProductRepository} del ejemplo
 * 5 de Inversion de Dependencias visto en la teoria: el servicio depende
 * de esta interfaz, nunca de una implementacion concreta como
 * {@link SqliteUserRepository}.
 */
public interface IUserRepository {

    boolean save(User newUser);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> list();

    boolean update(User user);
}
