package co.unicauca.saberpro.usuarios.domain.menu;

import co.unicauca.saberpro.usuarios.domain.Role;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resuelve el {@link IMenuProvider} adecuado para un rol dado. La UI solo
 * conoce este registro y la interfaz {@link IMenuProvider}: para soportar
 * un nuevo rol basta con crear una nueva implementacion y registrarla aqui
 * (en el punto de composicion de la aplicacion), sin modificar la ventana
 * del tablero ni las demas estrategias (OCP).
 */
public class MenuProviderRegistry {

    private final Map<Role, IMenuProvider> providersByRole;

    public MenuProviderRegistry(List<IMenuProvider> providers) {
        this.providersByRole = new EnumMap<>(Role.class);
        for (IMenuProvider provider : providers) {
            providersByRole.put(provider.getRole(), provider);
        }
    }

    public static MenuProviderRegistry withDefaultProviders() {
        return new MenuProviderRegistry(List.of(
                new AdministradorMenuProvider(),
                new AutorPreguntasMenuProvider(),
                new RevisorMenuProvider(),
                new DocenteMenuProvider(),
                new EstudianteMenuProvider()
        ));
    }

    public List<String> menuOptionsFor(Role role) {
        IMenuProvider provider = providersByRole.get(role);
        if (provider == null) {
            throw new IllegalStateException("No hay un IMenuProvider registrado para el rol " + role);
        }
        return provider.getMenuOptions();
    }
}
