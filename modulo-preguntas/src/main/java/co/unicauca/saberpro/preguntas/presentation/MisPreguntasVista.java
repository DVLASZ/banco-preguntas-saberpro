package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.Pagina;
import co.unicauca.saberpro.preguntas.domain.Question;

/**
 * Lo que el controlador necesita de la vista del listado "Mis preguntas": solo
 * saber pintar una página de resultados. Así el controlador no depende de
 * Swing y se puede probar con una vista falsa.
 */
public interface MisPreguntasVista {

    void mostrar(Pagina<Question> pagina);
}
