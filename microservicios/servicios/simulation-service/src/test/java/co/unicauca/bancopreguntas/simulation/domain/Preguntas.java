package co.unicauca.bancopreguntas.simulation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Preguntas de prueba. */
public final class Preguntas {

    public static final Instant PUBLICADA = Instant.parse("2026-10-10T10:00:00Z");

    private Preguntas() {
    }

    public static PreguntaDeCatalogo pregunta(String id, String competencia, String dificultad, String tema, char correcta) {
        return new PreguntaDeCatalogo(id, 1, "Pregunta " + id, competencia, tema, "Subtema " + id, dificultad,
                "Contexto de " + id, "¿Enunciado de " + id + "?", "Opción A", "Opción B", "Opción C", "Opción D",
                correcta, PUBLICADA, true);
    }

    public static PreguntaDeCatalogo pregunta(String id) {
        return pregunta(id, "LECTURA_CRITICA", "BASICO", "Idea principal", 'B');
    }

    /** {@code n} preguntas de lectura crítica con respuesta correcta B. */
    public static List<PreguntaDeCatalogo> lectura(int n) {
        List<PreguntaDeCatalogo> lista = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            lista.add(pregunta("q-" + i));
        }
        return lista;
    }

    public static CriteriosDeSimulacro criterios(int cantidad, int minutos) {
        return new CriteriosDeSimulacro(null, null, null, cantidad, minutos);
    }
}
