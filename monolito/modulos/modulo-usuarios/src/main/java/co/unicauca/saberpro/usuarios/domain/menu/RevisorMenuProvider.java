package co.unicauca.saberpro.usuarios.domain.menu;

import co.unicauca.saberpro.usuarios.domain.Role;

import java.util.List;

public class RevisorMenuProvider implements IMenuProvider {

    @Override
    public Role getRole() {
        return Role.REVISOR;
    }

    @Override
    public List<String> getMenuOptions() {
        return List.of(
                "Ver preguntas asignadas",
                "Diligenciar formato de evaluacion",
                "Aprobar o rechazar pregunta",
                "Ver historial de revisiones"
        );
    }
}
