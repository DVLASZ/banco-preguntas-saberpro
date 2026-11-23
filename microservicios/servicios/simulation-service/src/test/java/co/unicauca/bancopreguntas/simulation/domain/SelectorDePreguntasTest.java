package co.unicauca.bancopreguntas.simulation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** El contrato que cumple cualquier estrategia de selección, comprobado con la que hoy se usa. */
@DisplayName("SelectorDePreguntas (contrato de la estrategia)")
class SelectorDePreguntasTest {

    final SelectorDePreguntas selector = new SeleccionAleatoria(new Random(3));

    @Test
    void entregaSoloPreguntasQueEstabanEntreLasCandidatas() {
        List<PreguntaDeCatalogo> candidatas = Preguntas.lectura(12);

        assertThat(selector.elegir(candidatas, 5)).hasSize(5).isSubsetOf(candidatas);
    }

    @Test
    void nuncaEntregaMasQueLasCandidatas() {
        assertThat(selector.elegir(Preguntas.lectura(4), 9)).hasSize(4);
    }

    @Test
    void conCeroPreguntasPedidasEntregaUnaListaVacia() {
        assertThat(selector.elegir(Preguntas.lectura(4), 0)).isEmpty();
    }

    @Test
    void sinCandidatasEntregaUnaListaVacia() {
        assertThat(selector.elegir(List.of(), 3)).isEmpty();
    }

    @Test
    void noModificaLaListaDeCandidatas() {
        List<PreguntaDeCatalogo> candidatas = Preguntas.lectura(6);
        List<PreguntaDeCatalogo> copia = List.copyOf(candidatas);

        selector.elegir(candidatas, 3);

        assertThat(candidatas).isEqualTo(copia);
    }
}
