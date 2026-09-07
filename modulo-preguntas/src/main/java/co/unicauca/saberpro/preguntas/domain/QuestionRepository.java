package co.unicauca.saberpro.preguntas.domain;

import java.util.List;

/**
 * Contrato de persistencia de preguntas. El dominio depende de esta
 * abstracción, nunca de una implementación concreta (Inversión de
 * Dependencias).
 */
public interface QuestionRepository {

    List<Question> obtenerTodas();

    /** Retorna la pregunta con ese id, o {@code null} si no existe. */
    Question obtenerPorId(String id);

    void actualizar(Question pregunta);

    /** Persiste una pregunta nueva (su id todavía no debe existir). */
    void crear(Question pregunta);

    /** Genera un id único para una pregunta nueva (p. ej. "P-009"). */
    String generarNuevoId();
}
