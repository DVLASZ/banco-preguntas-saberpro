package co.unicauca.bancopreguntas.review.api;

import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import co.unicauca.bancopreguntas.review.api.dto.AssignReviewersRequest;
import co.unicauca.bancopreguntas.review.api.dto.AssignmentResponse;
import co.unicauca.bancopreguntas.review.api.dto.EvaluationRequest;
import co.unicauca.bancopreguntas.review.api.dto.EvaluationResultResponse;
import co.unicauca.bancopreguntas.review.api.dto.PendingCaseResponse;
import co.unicauca.bancopreguntas.review.api.dto.ReviewConfigResponse;
import co.unicauca.bancopreguntas.review.api.dto.ReviewPageResponse;
import co.unicauca.bancopreguntas.review.api.dto.ReviewResponse;
import co.unicauca.bancopreguntas.review.application.ReviewApplicationService;
import co.unicauca.bancopreguntas.review.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.review.domain.Observador;
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
 * Adaptador de entrada REST de la revisión por pares (HU-04 y HU-05). Cada operación exige su rol; la
 * identidad llega en un {@link AuthenticatedUser} (autorización redundante con el gateway).
 */
@RestController
@RequestMapping("/api/reviews")
@Validated
@Tag(name = "Revisiones", description = "Asignación de revisores y evaluación de preguntas")
public class ReviewController {

    private final ReviewApplicationService revisiones;

    public ReviewController(ReviewApplicationService revisiones) {
        this.revisiones = revisiones;
    }

    @GetMapping("/config")
    @Operation(summary = "Límites de revisores por pregunta. Cualquier usuario autenticado.")
    public ReviewConfigResponse configuracion(AuthenticatedUser usuario) {
        return new ReviewConfigResponse(1, revisiones.maximoDeRevisores());
    }

    @GetMapping("/pending-assignment")
    @Operation(summary = "Preguntas que esperan revisores (HU-04). Rol ADMIN.")
    public ReviewPageResponse<PendingCaseResponse> pendientesDeAsignacion(
            AuthenticatedUser usuario,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN);
        return ReviewPageResponse.de(revisiones.pendientesDeAsignacion(page + 1, size), PendingCaseResponse::de);
    }

    @PostMapping("/{questionId}/assignments")
    @Operation(summary = "Asignar revisores a la ronda vigente, o agregar más a una en curso (HU-04). Rol ADMIN.")
    public ReviewResponse asignar(AuthenticatedUser usuario, @PathVariable String questionId,
                                  @Valid @RequestBody AssignReviewersRequest solicitud) {
        usuario.requireAnyRole(Roles.ADMIN);
        return ReviewResponse.de(revisiones.asignarRevisores(questionId, solicitud.reviewerIds(), usuario.id())
                .vistaPara(new Observador(usuario.id(), true)));
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
        return ReviewPageResponse.de(revisiones.asignadasA(usuario.id(), estado, page + 1, size),
                AssignmentResponse::de);
    }

    @GetMapping("/{questionId}")
    @Operation(summary = "La revisión de una pregunta según quién consulta. ADMIN, AUTHOR o REVIEWER.",
            description = "El administrador y los revisores ven todo; el autor ve el detalle de las rondas ya decididas.")
    public ReviewResponse revision(AuthenticatedUser usuario, @PathVariable String questionId) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.AUTHOR, Roles.REVIEWER);
        return ReviewResponse.de(revisiones.caso(questionId,
                new Observador(usuario.id(), usuario.hasRole(Roles.ADMIN))));
    }

    @PostMapping("/{questionId}/evaluations")
    @Operation(summary = "Enviar mi evaluación de la pregunta (HU-05). Rol REVIEWER.",
            description = "Definitiva. La observación es obligatoria al rechazar. Si la ronda ya fue rechazada, "
                    + "queda como observación tardía.")
    public EvaluationResultResponse evaluar(AuthenticatedUser usuario, @PathVariable String questionId,
                                            @Valid @RequestBody EvaluationRequest solicitud) {
        usuario.requireAnyRole(Roles.REVIEWER);
        return EvaluationResultResponse.de(
                revisiones.evaluar(questionId, usuario.id(), solicitud.decision(), solicitud.observacion()));
    }
}
