package co.unicauca.bancopreguntas.validation.api;

import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import co.unicauca.bancopreguntas.validation.api.dto.ValidationRequest;
import co.unicauca.bancopreguntas.validation.api.dto.ValidationResponse;
import co.unicauca.bancopreguntas.validation.application.ValidationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST de la validación estructural de preguntas. Lo llama el servicio de preguntas (el gateway
 * no lo expone); como cualquier servicio, comprueba el rol de quien pidió la operación.
 */
@RestController
@RequestMapping("/api/validation")
@Tag(name = "Validación", description = "Reglas de calidad del contenido de una pregunta")
public class ValidationController {

    private final ValidationApplicationService validacion;

    public ValidationController(ValidationApplicationService validacion) {
        this.validacion = validacion;
    }

    @PostMapping("/questions")
    @Operation(summary = "Revisar el contenido de una pregunta contra las reglas de calidad. Rol AUTHOR o ADMIN.",
            description = "Siempre responde 200: el cuerpo dice si es válida y, si no, lista todos los campos que incumplen.")
    public ValidationResponse validar(AuthenticatedUser usuario, @RequestBody ValidationRequest contenido) {
        usuario.requireAnyRole(Roles.AUTHOR, Roles.ADMIN);
        return ValidationResponse.de(validacion.validar(contenido.aContenido()));
    }
}
