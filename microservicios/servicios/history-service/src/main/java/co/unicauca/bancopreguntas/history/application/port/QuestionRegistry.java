package co.unicauca.bancopreguntas.history.application.port;

import co.unicauca.bancopreguntas.history.domain.InfoPregunta;

import java.util.Collection;
import java.util.Optional;

/** Puerto de salida: quién es el autor de cada pregunta y quiénes la han revisado. */
public interface QuestionRegistry {

    /** Guarda la información de la pregunta, reemplazando la anterior. */
    void guardar(InfoPregunta info);

    Optional<InfoPregunta> buscar(String preguntaId);

    /** Registra a los revisores de la pregunta; los que ya estaban no se duplican. */
    void agregarRevisores(String preguntaId, Collection<String> revisores);

    /** Si el usuario es o fue revisor de la pregunta. */
    boolean esRevisor(String preguntaId, String usuarioId);
}
