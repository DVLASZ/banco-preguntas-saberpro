package co.unicauca.bancopreguntas.simulation.api.dto;

import co.unicauca.bancopreguntas.simulation.domain.ItemDeSimulacro;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Una pregunta del simulacro. {@code respuestaCorrecta} y {@code acertada} solo aparecen cuando el simulacro ya
 * terminó; mientras está en curso no se revela la respuesta.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimulationItemResponse(int orden, String preguntaId, String competencia, String tema, String dificultad,
                                     String contexto, String enunciado, String opcionA, String opcionB,
                                     String opcionC, String opcionD, String respuesta, String respuestaCorrecta,
                                     Boolean acertada) {

    public static SimulationItemResponse de(ItemDeSimulacro i, boolean revelar) {
        var p = i.pregunta();
        return new SimulationItemResponse(i.orden(), p.preguntaId(), p.competencia(), p.tema(), p.dificultad(),
                p.contexto(), p.enunciado(), p.opcionA(), p.opcionB(), p.opcionC(), p.opcionD(),
                i.respuesta() == null ? null : String.valueOf(i.respuesta()),
                revelar ? String.valueOf(p.respuestaCorrecta()) : null,
                revelar ? i.acertada() : null);
    }
}
