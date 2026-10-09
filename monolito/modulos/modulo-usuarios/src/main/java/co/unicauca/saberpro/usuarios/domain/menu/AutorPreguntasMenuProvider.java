package co.unicauca.saberpro.usuarios.domain.menu;

import co.unicauca.saberpro.usuarios.domain.Role;

import java.util.List;

public class AutorPreguntasMenuProvider implements IMenuProvider {

    @Override
    public Role getRole() {
        return Role.AUTOR_PREGUNTAS;
    }

    @Override
    public List<String> getMenuOptions() {
        return List.of(
                "Crear pregunta",
                "Editar mis preguntas en borrador",
                "Enviar pregunta a revision",
                "Consultar mis preguntas"
        );
    }
}
