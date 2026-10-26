package co.unicauca.bancopreguntas.validation.domain.validation;

import java.util.List;

/**
 * RF-13: las opciones respetan criterios básicos de longitud y estructura:
 * entre {@value #MINIMO} y {@value #MAXIMO} caracteres, comienzan con
 * mayúscula o dígito y no tienen espacios repetidos.
 */
public class LongitudYEstructuraOpcionesRule extends ReglaPorOpcion {

    static final int MINIMO = 3;
    static final int MAXIMO = 250;

    @Override
    protected void evaluar(int indice, String texto, List<Violacion> violaciones) {
        String opcion = texto.trim();
        if (opcion.length() < MINIMO || opcion.length() > MAXIMO) {
            violaciones.add(enOpcion(indice,
                    etiqueta(indice) + " debe tener entre " + MINIMO + " y " + MAXIMO + " caracteres"));
        }
        char primera = opcion.charAt(0);
        if (!Character.isUpperCase(primera) && !Character.isDigit(primera)) {
            violaciones.add(enOpcion(indice, etiqueta(indice) + " debe comenzar con mayúscula"));
        }
        if (opcion.contains("  ")) {
            violaciones.add(enOpcion(indice, etiqueta(indice) + " tiene espacios repetidos"));
        }
    }
}
