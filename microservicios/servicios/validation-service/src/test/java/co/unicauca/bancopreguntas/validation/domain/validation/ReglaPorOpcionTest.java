package co.unicauca.bancopreguntas.validation.domain.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReglaPorOpcion (patrón Template Method)")
class ReglaPorOpcionTest {

    /** Regla de prueba que registra qué opciones examina el esqueleto. */
    private static class Espia extends ReglaPorOpcion {
        final List<String> vistas = new ArrayList<>();
        boolean omitirTodo;

        @Override
        protected void evaluar(int indice, String opcion, List<Violacion> violaciones) {
            vistas.add(Texto.letra(indice) + "=" + opcion);
            violaciones.add(enOpcion(indice, etiqueta(indice) + " examinada"));
        }

        @Override
        protected boolean omitir(String opcion) {
            return omitirTodo || super.omitir(opcion);
        }
    }

    @Test
    @DisplayName("el esqueleto recorre A a D, omite las vacías y reúne las violaciones de cada paso")
    void recorreYOmiteLasVacias() {
        Espia regla = new Espia();

        List<Violacion> violaciones = regla.validar(ContenidoDePrueba.valido().opcionA("Uno").opcionB("  ").opcionC("Tres").opcionD(null).build());

        assertThat(regla.vistas).containsExactly("A=Uno", "C=Tres");
        assertThat(violaciones).extracting(Violacion::campo, Violacion::mensaje)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("opcionA", "La opción A examinada"),
                        org.assertj.core.groups.Tuple.tuple("opcionC", "La opción C examinada"));
    }

    @Test
    @DisplayName("el gancho omitir se puede cambiar sin tocar el esqueleto")
    void ganchoPersonalizado() {
        Espia regla = new Espia();
        regla.omitirTodo = true;

        assertThat(regla.validar(ContenidoDePrueba.valido().build())).isEmpty();
        assertThat(regla.vistas).isEmpty();
    }

    @Test
    @DisplayName("las reglas de expresiones prohibidas y de longitud usan el mismo esqueleto")
    void lasReglasRealesLoUsan() {
        assertThat(new ExpresionesProhibidasRule()).isInstanceOf(ReglaPorOpcion.class);
        assertThat(new LongitudYEstructuraOpcionesRule()).isInstanceOf(ReglaPorOpcion.class);
    }
}
