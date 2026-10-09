package co.unicauca.saberpro.usuarios.domain.access;

import co.unicauca.saberpro.usuarios.domain.Role;
import co.unicauca.saberpro.usuarios.domain.User;
import co.unicauca.saberpro.usuarios.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integracion del repositorio contra una base de datos SQLite en
 * memoria (se crea y se descarta en cada prueba), verificando que el
 * mapeo objeto-relacional y el esquema son correctos.
 */
class SqliteUserRepositoryTest {

    private IUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new SqliteUserRepository("jdbc:sqlite::memory:");
    }

    @Test
    void savedUserCanBeFoundByUsername() {
        User user = User.newUser("jperez", "Juan Perez", Role.ESTUDIANTE, UserStatus.ACTIVO, "hash123");

        boolean saved = userRepository.save(user);

        assertTrue(saved);
        assertNotNull(user.getId());
        Optional<User> found = userRepository.findByUsername("jperez");
        assertTrue(found.isPresent());
        assertEquals("Juan Perez", found.get().getFullName());
        assertEquals(Role.ESTUDIANTE, found.get().getRole());
        assertEquals(UserStatus.ACTIVO, found.get().getStatus());
    }

    @Test
    void existsByUsernameReflectsSavedUsers() {
        assertFalse(userRepository.existsByUsername("jperez"));
        userRepository.save(User.newUser("jperez", "Juan Perez", Role.ESTUDIANTE, UserStatus.ACTIVO, "hash123"));
        assertTrue(userRepository.existsByUsername("jperez"));
    }

    @Test
    void listReturnsAllSavedUsersOrderedByUsername() {
        userRepository.save(User.newUser("zulu", "Zulu User", Role.ESTUDIANTE, UserStatus.ACTIVO, "hash"));
        userRepository.save(User.newUser("alfa", "Alfa User", Role.DOCENTE, UserStatus.ACTIVO, "hash"));

        List<User> all = userRepository.list();

        assertEquals(2, all.size());
        assertEquals("alfa", all.get(0).getUsername());
        assertEquals("zulu", all.get(1).getUsername());
    }

    @Test
    void updateShouldPersistChanges() {
        User user = User.newUser("jperez", "Juan Perez", Role.ESTUDIANTE, UserStatus.ACTIVO, "hash123");
        userRepository.save(user);

        user.deactivate();
        boolean updated = userRepository.update(user);

        assertTrue(updated);
        Optional<User> reloaded = userRepository.findByUsername("jperez");
        assertTrue(reloaded.isPresent());
        assertEquals(UserStatus.INACTIVO, reloaded.get().getStatus());
    }
}
