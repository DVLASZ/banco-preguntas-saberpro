package co.unicauca.saberpro.usuarios.domain.menu;

import co.unicauca.saberpro.usuarios.domain.Role;

import java.util.List;

public class AdministradorMenuProvider implements IMenuProvider {

    @Override
    public Role getRole() {
        return Role.ADMINISTRADOR;
    }

    @Override
    public List<String> getMenuOptions() {
        return List.of(
                "Gestionar usuarios",
                "Ver banco de preguntas",
                "Ver reportes y estadisticas",
                "Configurar el sistema"
        );
    }
}
