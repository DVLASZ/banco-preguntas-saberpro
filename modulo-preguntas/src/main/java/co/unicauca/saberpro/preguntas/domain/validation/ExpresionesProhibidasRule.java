package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * RF-12: las opciones no pueden usar expresiones como "todas las anteriores"
 * o "ninguna de las anteriores". La comparación ignora mayúsculas y tildes.
 */
public class ExpresionesProhibidasRule implements ValidationRule {

    private static final List<Pattern> PROHIBIDAS = List.of(
            Pattern.compile("\\btodas\\s+las\\s+(anteriores|opciones)\\b"),
            Pattern.compile("\\bninguna\\s+de\\s+las\\s+(anteriores|opciones)\\b"));

    @Override
    public List<Violacion> validar(ContenidoPregunta contenido) {
        List<String> opciones = Texto.opciones(contenido);
        List<Violacion> violaciones = new ArrayList<>();
        for (int i = 0; i < opciones.size(); i++) {
            String normalizada = Texto.normalizar(opciones.get(i));
            for (Pattern prohibida : PROHIBIDAS) {
                if (prohibida.matcher(normalizada).find()) {
                    violaciones.add(new Violacion(Texto.campoOpcion(i), "La opción " + Texto.letra(i)
                            + " usa una expresión no permitida (\"todas/ninguna de las anteriores\")"));
                    break;
                }
            }
        }
        return violaciones;
    }
}
