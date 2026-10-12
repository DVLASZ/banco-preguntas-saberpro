package co.unicauca.bancopreguntas.notification.application.port;

import co.unicauca.bancopreguntas.notification.domain.InfoPregunta;

import java.util.Optional;

/** Puerto de salida: lo que este servicio recuerda de cada pregunta (título y autor). */
public interface QuestionInfoRepository {

    /** Guarda la información, reemplazando la anterior de la misma pregunta. */
    void guardar(InfoPregunta info);

    Optional<InfoPregunta> buscar(String preguntaId);
}
