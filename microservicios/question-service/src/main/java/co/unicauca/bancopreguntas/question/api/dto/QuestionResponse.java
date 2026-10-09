package co.unicauca.bancopreguntas.question.api.dto;

import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.Competencia;
import co.unicauca.bancopreguntas.question.domain.Dificultad;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.Question;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Una pregunta tal como la devuelve el API. El historial de estados solo viaja en la consulta por id. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionResponse(
        String id,
        String autorId,
        String nombre,
        String contexto,
        String enunciado,
        String opcionA,
        String opcionB,
        String opcionC,
        String opcionD,
        String respuestaCorrecta,
        String justificacion,
        String bibliografia,
        Competencia competencia,
        String tema,
        String subtema,
        Dificultad dificultad,
        EstadoPregunta estado,
        int version,
        Instant creadaEn,
        Instant actualizadaEn,
        List<CambioEstadoResponse> historialEstados) {

    public static QuestionResponse de(Question p) {
        return de(p, null);
    }

    public static QuestionResponse de(Question p, List<CambioEstado> historial) {
        return new QuestionResponse(
                p.getId(), p.getAutor(), p.getNombre(), p.getContexto(), p.getEnunciado(),
                p.getOpciones().getOpcionA(), p.getOpciones().getOpcionB(),
                p.getOpciones().getOpcionC(), p.getOpciones().getOpcionD(),
                String.valueOf(p.getRespuestaCorrecta()), p.getJustificacion(), p.getBibliografia(),
                p.getCompetencia(), p.getTema(), p.getSubtema(), p.getDificultad(), p.getEstado(),
                p.getVersion(), p.getCreadaEn(), p.getActualizadaEn(),
                historial == null ? null : historial.stream().map(CambioEstadoResponse::de).toList());
    }
}
