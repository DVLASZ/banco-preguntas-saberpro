package co.unicauca.saberpro.revision.domain;

import co.unicauca.saberpro.usuarios.domain.Role;
import co.unicauca.saberpro.usuarios.domain.User;
import co.unicauca.saberpro.usuarios.domain.UserStatus;
import co.unicauca.saberpro.usuarios.domain.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Pruebas de {@link DirectorioRevisoresDeUsuarios}. */
@ExtendWith(MockitoExtension.class)
class DirectorioRevisoresDeUsuariosTest {

    @Mock
    private UserService userService;

    private DirectorioRevisoresDeUsuarios directorio;

    @BeforeEach
    void setUp() {
        directorio = new DirectorioRevisoresDeUsuarios(userService);
    }

    @Test
    void revisoresActivos_soloDevuelveUsuariosConRolRevisorYEstadoActivo() {
        User revisorActivo = new User(1L, "revisor1", "Roberto Revisor", Role.REVISOR, UserStatus.ACTIVO, "hash");
        User revisorInactivo = new User(2L, "revisor2", "Rita Revisora", Role.REVISOR, UserStatus.INACTIVO, "hash");
        User otroRol = new User(3L, "autor1", "Ana Autora", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, "hash");
        when(userService.listUsers()).thenReturn(List.of(revisorActivo, revisorInactivo, otroRol));

        List<Revisor> resultado = directorio.revisoresActivos();

        assertEquals(List.of(new Revisor("revisor1", "Roberto Revisor")), resultado);
    }

    @Test
    void revisoresActivos_devuelveUsuarioYNombreCompleto() {
        User revisorActivo = new User(1L, "revisor1", "Roberto Revisor", Role.REVISOR, UserStatus.ACTIVO, "hash");
        when(userService.listUsers()).thenReturn(List.of(revisorActivo));

        List<Revisor> resultado = directorio.revisoresActivos();

        assertEquals(1, resultado.size());
        assertEquals("revisor1", resultado.get(0).usuario());
        assertEquals("Roberto Revisor", resultado.get(0).nombreCompleto());
    }
}