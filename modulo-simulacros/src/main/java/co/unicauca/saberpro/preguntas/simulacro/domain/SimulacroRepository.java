package co.unicauca.saberpro.preguntas.simulacro.domain;

import java.util.List;

/**
 * Contrato de persistencia de simulacros e intentos. El dominio depende
 * de esta abstracción, nunca de una implementación concreta (Inversión
 * de Dependencias) — igual que {@code QuestionRepository}.
 */
public interface SimulacroRepository {

    void guardarSimulacro(Simulacro simulacro);

    Simulacro obtenerSimulacro(String id);

    List<Simulacro> listarSimulacros();

    /** Genera un id único para un simulacro nuevo (p. ej. "S-003"). */
    String generarNuevoIdSimulacro();

    void guardarIntento(IntentoSimulacro intento);

    IntentoSimulacro obtenerIntento(String id);

    List<IntentoSimulacro> listarIntentosDeEstudiante(String estudianteUsername);

    /** Genera un id único para un intento nuevo (p. ej. "I-003"). */
    String generarNuevoIdIntento();
}
