package co.unicauca.bancopreguntas.simulation.api.dto;

import co.unicauca.bancopreguntas.simulation.domain.CriteriosDeSimulacro;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Set;

/** Lo que pide el estudiante al generar un simulacro. Los conjuntos vacíos significan «cualquiera». */
public record CreateSimulationRequest(
        @Schema(description = "Competencias, por ejemplo LECTURA_CRITICA", example = "[\"LECTURA_CRITICA\"]") Set<String> competencias,
        @Schema(description = "Temas o subtemas") Set<String> temas,
        @Schema(description = "Niveles: BASICO, INTERMEDIO o AVANZADO") Set<String> dificultades,
        @Min(value = 1, message = "cantidad debe ser al menos 1") @Max(value = 100, message = "cantidad no puede superar 100") int cantidad,
        @Min(value = 1, message = "duracionMinutos debe ser al menos 1")
        @Max(value = CriteriosDeSimulacro.DURACION_MAXIMA_MINUTOS, message = "duracionMinutos no puede superar 240") int duracionMinutos) {

    public CriteriosDeSimulacro aCriterios() {
        return new CriteriosDeSimulacro(competencias, temas, dificultades, cantidad, duracionMinutos);
    }
}
