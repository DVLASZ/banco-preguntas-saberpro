package co.unicauca.bancopreguntas.validation.domain.validation;

import co.unicauca.bancopreguntas.validation.domain.ContenidoPregunta;

import java.util.ArrayList;
import java.util.List;

/**
 * Patrón Template Method: el esqueleto de las reglas que examinan las cuatro opciones de respuesta. {@link #validar}
 * recorre las opciones A a D, salta las que {@link #omitir} descarta y deja a cada subclase el paso que cambia,
 * {@link #evaluar}. Así las reglas por opción no repiten el recorrido ni la construcción de los mensajes.
 */
public abstract class ReglaPorOpcion implements ValidationRule {

    /** El esqueleto del algoritmo: no se redefine en las subclases. */
    @Override
    public final List<Violacion> validar(ContenidoPregunta contenido) {
        List<String> opciones = Texto.opciones(contenido);
        List<Violacion> violaciones = new ArrayList<>();
        for (int i = 0; i < opciones.size(); i++) {
            if (!omitir(opciones.get(i))) {
                evaluar(i, opciones.get(i), violaciones);
            }
        }
        return violaciones;
    }

    /** Paso variable: examina una opción y agrega sus violaciones. {@code indice} es 0 para A, 1 para B... */
    protected abstract void evaluar(int indice, String opcion, List<Violacion> violaciones);

    /** Gancho: por defecto se omiten las opciones vacías (de ellas se ocupa la regla de las cuatro opciones). */
    protected boolean omitir(String opcion) {
        return Texto.vacio(opcion);
    }

    /** Una violación en la opción de la posición dada. */
    protected static Violacion enOpcion(int indice, String mensaje) {
        return new Violacion(Texto.campoOpcion(indice), mensaje);
    }

    /** «La opción X», para los mensajes. */
    protected static String etiqueta(int indice) {
        return "La opción " + Texto.letra(indice);
    }
}
