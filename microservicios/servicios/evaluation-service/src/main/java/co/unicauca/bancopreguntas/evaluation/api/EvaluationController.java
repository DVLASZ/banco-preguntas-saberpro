package co.unicauca.bancopreguntas.evaluation.api;

import co.unicauca.bancopreguntas.evaluation.api.dto.EvaluationRequest;
import co.unicauca.bancopreguntas.evaluation.api.dto.EvaluationResultResponse;
import co.unicauca.bancopreguntas.evaluation.api.dto.ReviewResponse;
import co.unicauca.bancopreguntas.evaluation.application.EvaluationApplicationService;
import co.unicauca.bancopreguntas.evaluation.domain.Observador;
import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST de la evaluación por pares (HU-05). Cada operación exige su rol; la identidad llega en un
 * {@link AuthenticatedUser} (autorización redundante con el gateway).
 */
@RestController
@RequestMapping("/api/reviews")
@Validated
@Tag(name = "Evaluaciones", description = "Evaluación de preguntas por los revisores y consulta de la revisión")
public class EvaluationController {

    private final EvaluationApplicationService evaluaciones;

    public EvaluationController(EvaluationApplicationService evaluaciones) {
        this.evaluaciones = evaluaciones;
    }

    @GetMapping("/{questionId}")
    @Operation(summary = "La revisión de una pregunta según quién consulta. ADMIN, AUTHOR o REVIEWER.",
            description = "El administrador y los revisores ven todo; el autor ve el detalle de las rondas ya decididas.")
    public ReviewResponse revision(AuthenticatedUser usuario, @PathVariable String questionId) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.AUTHOR, Roles.REVIEWER);
        return ReviewResponse.de(evaluaciones.caso(questionId,
                new Observador(usuario.id(), usuario.hasRole(Roles.ADMIN))));
    }

    @PostMapping("/{questionId}/evaluations")
    @Operation(summary = "Enviar mi evaluación de la pregunta (HU-05). Rol REVIEWER.",
            description = "Definitiva. La observación es obligatoria al rechazar. Si la ronda ya fue rechazada, "
                    + "queda como observación tardía. Publica review.evaluation-submitted y, si decide la ronda, "
                    + "review.completed.")
    public EvaluationResultResponse evaluar(AuthenticatedUser usuario, @PathVariable String questionId,
                                            @Valid @RequestBody EvaluationRequest solicitud) {
        usuario.requireAnyRole(Roles.REVIEWER);
        return EvaluationResultResponse.de(
                evaluaciones.evaluar(questionId, usuario.id(), solicitud.decision(), solicitud.observacion()));
    }
}
