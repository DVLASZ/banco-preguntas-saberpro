package co.unicauca.bancopreguntas.simulation.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Escoge las preguntas al azar y las deja en un orden aleatorio. El generador se inyecta para poder probarla. */
public class SeleccionAleatoria implements SelectorDePreguntas {

    private final Random azar;

    public SeleccionAleatoria(Random azar) {
        this.azar = azar;
    }

    @Override
    public List<PreguntaDeCatalogo> elegir(List<PreguntaDeCatalogo> candidatas, int cantidad) {
        List<PreguntaDeCatalogo> barajadas = new ArrayList<>(candidatas);
        Collections.shuffle(barajadas, azar);
        return List.copyOf(barajadas.subList(0, Math.min(cantidad, barajadas.size())));
    }
}
