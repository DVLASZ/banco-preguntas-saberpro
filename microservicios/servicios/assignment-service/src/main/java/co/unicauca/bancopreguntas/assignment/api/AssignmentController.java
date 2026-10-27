package co.unicauca.bancopreguntas.assignment.api;

import co.unicauca.bancopreguntas.assignment.api.dto.AssignReviewersRequest;
import co.unicauca.bancopreguntas.assignment.api.dto.AssignmentResponse;
import co.unicauca.bancopreguntas.assignment.api.dto.CaseAssignmentsResponse;
import co.unicauca.bancopreguntas.assignment.api.dto.PendingCaseResponse;
import co.unicauca.bancopreguntas.assignment.api.dto.ReviewConfigResponse;
import co.unicauca.bancopreguntas.assignment.api.dto.ReviewPageResponse;
import co.unicauca.bancopreguntas.assignment.application.AssignmentApplicationService;
import co.unicauca.bancopreguntas.assignment.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST de la asignación de revisores (HU-04) y de la bandeja del revisor. Cada operación exige su
 * rol; la identidad llega en un {@link AuthenticatedUser} (autorización redundante con el gateway).
 */
@RestController
@RequestMapping("/api/reviews")
@Validated
@Tag(name = "Asignación de revisores", description = "Asignación de revisores a las preguntas y bandeja del revisor")
public class AssignmentController {

    private final AssignmentApplicationService asignaciones;

    public AssignmentController(AssignmentApplicationService asignaciones) {
        this.asignaciones = asignaciones;
    }

    @GetMapping("/config")
    @Operation(summary = "Límites de revisores por pregunta. Cualquier usuario autenticado.")
    public ReviewConfigResponse configuracion(AuthenticatedUser usuario) {
        return new ReviewConfigResponse(1, asignaciones.maximoDeRevisores());
    }

    @GetMapping("/pending-assignment")
    @Operation(summary = "Preguntas que esperan revisores (HU-04). Rol ADMIN.")
    public ReviewPageResponse<PendingCaseResponse> pendientesDeAsignacion(
            AuthenticatedUser usuario,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN);
        return ReviewPageResponse.de(asignaciones.pendientesDeAsignacion(page + 1, size), PendingCaseResponse::de);
    }

    @PostMapping("/{questionId}/assignments")
    @Operation(summary = "Asignar revisores a la ronda vigente, o agregar más a una en curso (HU-04). Rol ADMIN.",
            description = "Publica review.reviewers-assigned con los revisores asignados en este acto.")
    public CaseAssignmentsResponse asignar(AuthenticatedUser usuario, @PathVariable String questionId,
                                           @Valid @RequestBody AssignReviewersRequest solicitud) {
        usuario.requireAnyRole(Roles.ADMIN);
        return CaseAssignmentsResponse.de(asignaciones.asignarRevisores(questionId, solicitud.reviewerIds(), usuario.id()));
    }

    @GetMapping("/assigned")
    @Operation(summary = "Mi bandeja de revisión (HU-05). Rol REVIEWER.",
            description = "estado: PENDIENTES (por defecto), EVALUADAS o TODAS.")
    public ReviewPageResponse<AssignmentResponse> asignadas(
            AuthenticatedUser usuario,
            @RequestParam(defaultValue = "PENDIENTES") FiltroAsignacion estado,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.REVIEWER);
        return ReviewPageResponse.de(asignaciones.asignadasA(usuario.id(), estado, page + 1, size),
                AssignmentResponse::de);
    }
}
