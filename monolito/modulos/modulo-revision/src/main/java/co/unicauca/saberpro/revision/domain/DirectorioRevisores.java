package co.unicauca.saberpro.revision.domain;

import java.util.List;

/** De dónde salen los revisores: en el proyecto, los usuarios con rol Revisor y estado Activo. */
public interface DirectorioRevisores {

    List<Revisor> revisoresActivos();
}
