package co.unicauca.bancopreguntas.validation.api.dto;

import co.unicauca.bancopreguntas.validation.domain.validation.Violacion;

import java.util.List;

/** Resultado de la revisión: si el contenido es válido y, si no, cada campo que incumple con su mensaje. */
public record ValidationResponse(boolean valida, List<ViolationResponse> violaciones) {

    public static ValidationResponse de(List<Violacion> violaciones) {
        return new ValidationResponse(violaciones.isEmpty(),
                violaciones.stream().map(v -> new ViolationResponse(v.campo(), v.mensaje())).toList());
    }

    /** Un campo que incumple las reglas y lo que debe corregir el autor. */
    public record ViolationResponse(String campo, String mensaje) {
    }
}
