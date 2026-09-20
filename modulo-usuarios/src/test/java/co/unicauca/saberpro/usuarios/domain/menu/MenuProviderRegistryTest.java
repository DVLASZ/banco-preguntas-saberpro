package co.unicauca.saberpro.usuarios.domain.menu;

import co.unicauca.saberpro.usuarios.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Los menús por rol y el registro que elige el que corresponde (patrón Strategy + Registry). */
class MenuProviderRegistryTest {

    private static final Map<IMenuProvider, Role> PROVEEDORES = Map.of(
            new AdministradorMenuProvider(), Role.ADMINISTRADOR,
            new AutorPreguntasMenuProvider(), Role.AUTOR_PREGUNTAS,
            new RevisorMenuProvider(), Role.REVISOR,
            new DocenteMenuProvider(), Role.DOCENTE,
            new EstudianteMenuProvider(), Role.ESTUDIANTE);

    @Test
    void cadaProveedor_declaraSuRolYTieneOpcionesSinRepetir() {
        PROVEEDORES.forEach((proveedor, rol) -> {
            assertEquals(rol, proveedor.getRole());
            List<String> opciones = proveedor.getMenuOptions();
            assertFalse(opciones.isEmpty(), "El menú de " + rol + " no puede estar vacío");
            assertTrue(opciones.stream().noneMatch(String::isBlank));
            assertEquals(opciones.size(), new HashSet<>(opciones).size(), "Opciones repetidas en " + rol);
        });
    }

    @Test
    void conProveedoresPorDefecto_todosLosRolesTienenMenu() {
        MenuProviderRegistry registro = MenuProviderRegistry.withDefaultProviders();

        for (Role rol : Role.values()) {
            assertFalse(registro.menuOptionsFor(rol).isEmpty(), "Sin menú para " + rol);
        }
    }

    @Test
    void menuOptionsFor_devuelveLasOpcionesDelRolPedido() {
        MenuProviderRegistry registro = MenuProviderRegistry.withDefaultProviders();

        assertEquals(new RevisorMenuProvider().getMenuOptions(), registro.menuOptionsFor(Role.REVISOR));
        assertEquals(new AdministradorMenuProvider().getMenuOptions(), registro.menuOptionsFor(Role.ADMINISTRADOR));
        assertNotEquals(registro.menuOptionsFor(Role.REVISOR), registro.menuOptionsFor(Role.ESTUDIANTE));
    }

    @Test
    void menuOptionsFor_sinProveedorParaElRol_falla() {
        MenuProviderRegistry registro = new MenuProviderRegistry(List.of(new RevisorMenuProvider()));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> registro.menuOptionsFor(Role.DOCENTE));

        assertTrue(error.getMessage().contains("Docente"));
    }

    @Test
    void dosProveedoresDelMismoRol_prevaleceElUltimo() {
        IMenuProvider personalizado = new IMenuProvider() {
            @Override
            public Role getRole() {
                return Role.REVISOR;
            }

            @Override
            public List<String> getMenuOptions() {
                return List.of("Solo esta opción");
            }
        };
        MenuProviderRegistry registro = new MenuProviderRegistry(List.of(new RevisorMenuProvider(), personalizado));

        assertEquals(List.of("Solo esta opción"), registro.menuOptionsFor(Role.REVISOR));
    }
}
