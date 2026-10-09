package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.util.ArrayList;
import java.util.List;

/**
 * RF-13: las opciones respetan criterios básicos de longitud y estructura:
 * entre {@value #MINIMO} y {@value #MAXIMO} caracteres, comienzan con
 * mayúscula o dígito y no tienen espacios repetidos.
 */
public class LongitudYEstructuraOpcionesRule implements ValidationRule {

    static final int MINIMO = 3;
    static final int MAXIMO = 250;

    @Override
    public List<Violacion> validar(ContenidoPregunta contenido) {
        List<String> opciones = Texto.opciones(contenido);
        List<Violacion> violaciones = new ArrayList<>();
        for (int i = 0; i < opciones.size(); i++) {
            String opcion = opciones.get(i).trim();
            if (opcion.isEmpty()) {
                continue;
            }
            String etiqueta = "La opción " + Texto.letra(i);
            if (opcion.length() < MINIMO || opcion.length() > MAXIMO) {
                violaciones.add(new Violacion(Texto.campoOpcion(i), etiqueta
                        + " debe tener entre " + MINIMO + " y " + MAXIMO + " caracteres"));
            }
            char primera = opcion.charAt(0);
            if (!Character.isUpperCase(primera) && !Character.isDigit(primera)) {
                violaciones.add(new Violacion(Texto.campoOpcion(i), etiqueta + " debe comenzar con mayúscula"));
            }
            if (opcion.contains("  ")) {
                violaciones.add(new Violacion(Texto.campoOpcion(i), etiqueta + " tiene espacios repetidos"));
            }
        }
        return violaciones;
    }
}
