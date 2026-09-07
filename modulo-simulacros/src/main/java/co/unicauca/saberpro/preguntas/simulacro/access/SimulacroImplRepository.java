package co.unicauca.saberpro.preguntas.simulacro.access;

import co.unicauca.saberpro.preguntas.simulacro.domain.IntentoSimulacro;
import co.unicauca.saberpro.preguntas.simulacro.domain.Simulacro;
import co.unicauca.saberpro.preguntas.simulacro.domain.SimulacroRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación en memoria de {@link SimulacroRepository}, siguiendo el
 * mismo criterio que {@code QuestionImplRepository}: no se requiere una
 * base de datos relacional para este taller.
 */
public class SimulacroImplRepository implements SimulacroRepository {

    private final Map<String, Simulacro> simulacros = new LinkedHashMap<>();
    private final Map<String, IntentoSimulacro> intentos = new LinkedHashMap<>();
    private int consecutivoSimulacro = 0;
    private int consecutivoIntento = 0;

    @Override
    public void guardarSimulacro(Simulacro simulacro) {
        simulacros.put(simulacro.getId(), simulacro);
    }

    @Override
    public Simulacro obtenerSimulacro(String id) {
        return simulacros.get(id);
    }

    @Override
    public List<Simulacro> listarSimulacros() {
        return new ArrayList<>(simulacros.values());
    }

    @Override
    public String generarNuevoIdSimulacro() {
        consecutivoSimulacro++;
        return String.format("S-%03d", consecutivoSimulacro);
    }

    @Override
    public void guardarIntento(IntentoSimulacro intento) {
        intentos.put(intento.getId(), intento);
    }

    @Override
    public IntentoSimulacro obtenerIntento(String id) {
        return intentos.get(id);
    }

    @Override
    public List<IntentoSimulacro> listarIntentosDeEstudiante(String estudianteUsername) {
        List<IntentoSimulacro> resultado = new ArrayList<>();
        for (IntentoSimulacro intento : intentos.values()) {
            if (intento.getEstudianteUsername().equalsIgnoreCase(estudianteUsername)) {
                resultado.add(intento);
            }
        }
        return resultado;
    }

    @Override
    public String generarNuevoIdIntento() {
        consecutivoIntento++;
        return String.format("I-%03d", consecutivoIntento);
    }
}
