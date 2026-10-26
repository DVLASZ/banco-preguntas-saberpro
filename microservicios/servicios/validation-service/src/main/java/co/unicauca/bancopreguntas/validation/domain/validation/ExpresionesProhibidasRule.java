package co.unicauca.bancopreguntas.validation.domain.validation;

import java.util.List;
import java.util.regex.Pattern;

/**
 * RF-12: las opciones no pueden usar expresiones como "todas las anteriores"
 * o "ninguna de las anteriores". La comparación ignora mayúsculas y tildes.
 */
public class ExpresionesProhibidasRule extends ReglaPorOpcion {

    private static final List<Pattern> PROHIBIDAS = List.of(
            Pattern.compile("\\btodas\\s+las\\s+(anteriores|opciones)\\b"),
            Pattern.compile("\\bninguna\\s+de\\s+las\\s+(anteriores|opciones)\\b"));

    @Override
    protected void evaluar(int indice, String opcion, List<Violacion> violaciones) {
        String normalizada = Texto.normalizar(opcion);
        if (PROHIBIDAS.stream().anyMatch(p -> p.matcher(normalizada).find())) {
            violaciones.add(enOpcion(indice,
                    etiqueta(indice) + " usa una expresión no permitida (\"todas/ninguna de las anteriores\")"));
        }
    }
}
