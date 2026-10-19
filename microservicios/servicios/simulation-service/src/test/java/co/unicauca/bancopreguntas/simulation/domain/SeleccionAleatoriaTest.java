package co.unicauca.bancopreguntas.simulation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SeleccionAleatoria")
class SeleccionAleatoriaTest {

    @Test
    @DisplayName("escoge la cantidad pedida sin repetir preguntas")
    void cantidad() {
        List<PreguntaDeCatalogo> elegidas = new SeleccionAleatoria(new Random(1)).elegir(Preguntas.lectura(20), 7);

        assertThat(elegidas).hasSize(7).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("si hay menos candidatas que las pedidas entrega todas")
    void pocas() {
        assertThat(new SeleccionAleatoria(new Random(1)).elegir(Preguntas.lectura(3), 10)).hasSize(3);
    }

    @Test
    @DisplayName("con la misma semilla escoge lo mismo y con otra, otro orden")
    void semilla() {
        List<PreguntaDeCatalogo> candidatas = Preguntas.lectura(30);

        List<PreguntaDeCatalogo> a = new SeleccionAleatoria(new Random(7)).elegir(candidatas, 10);
        List<PreguntaDeCatalogo> b = new SeleccionAleatoria(new Random(7)).elegir(candidatas, 10);
        List<PreguntaDeCatalogo> c = new SeleccionAleatoria(new Random(8)).elegir(candidatas, 10);

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    @DisplayName("no modifica la lista de candidatas")
    void noModifica() {
        List<PreguntaDeCatalogo> candidatas = Preguntas.lectura(10);
        List<PreguntaDeCatalogo> copia = List.copyOf(candidatas);

        new SeleccionAleatoria(new Random(3)).elegir(candidatas, 5);

        assertThat(candidatas).isEqualTo(copia);
    }
}
