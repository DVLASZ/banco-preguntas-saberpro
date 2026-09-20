package co.unicauca.saberpro.preguntas.domain.validation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;

import java.util.ArrayList;
import java.util.List;

/**
 * RF-10: existen exactamente cuatro opciones de respuesta (A a D), todas
 * con contenido y distintas entre sí (dos opciones iguales harían ambigua
 * la respuesta correcta).
 */
public class CuatroOpcionesRule implements ValidationRule {

    @Override
    public List<Violacion> validar(ContenidoPregunta contenido) {
        List<String> opciones = Texto.opciones(contenido);
        List<Violacion> violaciones = new ArrayList<>();
        for (int i = 0; i < opciones.size(); i++) {
            if (Texto.vacio(opciones.get(i))) {
                violaciones.add(new Violacion(Texto.campoOpcion(i),
                        "La opción " + Texto.letra(i) + " es obligatoria"));
            }
        }
        for (int i = 0; i < opciones.size(); i++) {
            for (int j = 0; j < i; j++) {
                if (!Texto.vacio(opciones.get(i))
                        && Texto.normalizar(opciones.get(i)).equals(Texto.normalizar(opciones.get(j)))) {
                    violaciones.add(new Violacion(Texto.campoOpcion(i),
                            "La opción " + Texto.letra(i) + " repite el contenido de la opción " + Texto.letra(j)));
                    break;
                }
            }
        }
        return violaciones;
    }
}
