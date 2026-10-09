package co.unicauca.saberpro.usuarios.domain.menu;

import co.unicauca.saberpro.usuarios.domain.Role;

import java.util.List;

public class EstudianteMenuProvider implements IMenuProvider {

    @Override
    public Role getRole() {
        return Role.ESTUDIANTE;
    }

    @Override
    public List<String> getMenuOptions() {
        return List.of(
                "Realizar simulacro",
                "Ver mi historial de simulacros",
                "Ver mis estadisticas y fortalezas/debilidades"
        );
    }
}
