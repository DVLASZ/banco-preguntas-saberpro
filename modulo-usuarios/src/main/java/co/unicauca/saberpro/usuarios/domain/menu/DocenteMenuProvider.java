package co.unicauca.saberpro.usuarios.domain.menu;

import co.unicauca.saberpro.usuarios.domain.Role;

import java.util.List;

public class DocenteMenuProvider implements IMenuProvider {

    @Override
    public Role getRole() {
        return Role.DOCENTE;
    }

    @Override
    public List<String> getMenuOptions() {
        return List.of(
                "Generar simulacro para el grupo",
                "Ver reportes de estudiantes",
                "Ver estadisticas por competencia"
        );
    }
}
