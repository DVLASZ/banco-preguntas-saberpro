package co.unicauca.saberpro.usuarios.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** La entidad usuario: sus datos, su estado y su igualdad. */
class UserTest {

    private static User usuario() {
        return new User(7L, "jperez", "Juan Perez", Role.REVISOR, UserStatus.ACTIVO, "hash123");
    }

    @Test
    void constructor_guardaTodosSusCampos() {
        User user = usuario();

        assertEquals(7L, user.getId());
        assertEquals("jperez", user.getUsername());
        assertEquals("Juan Perez", user.getFullName());
        assertEquals(Role.REVISOR, user.getRole());
        assertEquals(UserStatus.ACTIVO, user.getStatus());
        assertEquals("hash123", user.getPasswordHash());
    }

    @Test
    void constructor_rechazaCamposNulos() {
        assertThrows(NullPointerException.class,
                () -> new User(1L, null, "Juan", Role.REVISOR, UserStatus.ACTIVO, "h"));
        assertThrows(NullPointerException.class,
                () -> new User(1L, "jperez", null, Role.REVISOR, UserStatus.ACTIVO, "h"));
        assertThrows(NullPointerException.class,
                () -> new User(1L, "jperez", "Juan", null, UserStatus.ACTIVO, "h"));
        assertThrows(NullPointerException.class,
                () -> new User(1L, "jperez", "Juan", Role.REVISOR, null, "h"));
        assertThrows(NullPointerException.class,
                () -> new User(1L, "jperez", "Juan", Role.REVISOR, UserStatus.ACTIVO, null));
    }

    @Test
    void newUser_nace_sinIdentificador_y_seLeAsignaDespues() {
        User user = User.newUser("jperez", "Juan Perez", Role.DOCENTE, UserStatus.ACTIVO, "hash");

        assertNull(user.getId());

        user.setId(3L);

        assertEquals(3L, user.getId());
    }

    @Test
    void activarYDesactivar_cambianElEstado() {
        User user = usuario();
        assertTrue(user.isActive());

        user.deactivate();
        assertEquals(UserStatus.INACTIVO, user.getStatus());
        assertFalse(user.isActive());

        user.activate();
        assertEquals(UserStatus.ACTIVO, user.getStatus());
        assertTrue(user.isActive());
    }

    @Test
    void igualdad_comparaElUsernameSinDistinguirMayusculas() {
        User a = new User(1L, "JPerez", "Juan", Role.REVISOR, UserStatus.ACTIVO, "h1");
        User b = new User(2L, "jperez", "Otro Nombre", Role.DOCENTE, UserStatus.INACTIVO, "h2");
        User c = new User(3L, "mlopez", "Maria", Role.REVISOR, UserStatus.ACTIVO, "h3");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "jperez");
    }

    @Test
    void toString_noExponeElHashDeLaContrasena() {
        String texto = usuario().toString();

        assertTrue(texto.contains("jperez"));
        assertFalse(texto.contains("hash123"));
    }

    @Test
    void role_muestraSuNombreLegible() {
        assertEquals("Autor de preguntas", Role.AUTOR_PREGUNTAS.getDisplayName());
        assertEquals("Administrador", Role.ADMINISTRADOR.toString());
        assertEquals(5, Role.values().length);
    }
}
