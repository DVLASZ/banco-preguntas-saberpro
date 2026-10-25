package co.unicauca.bancopreguntas.email.api;

import co.unicauca.bancopreguntas.email.api.dto.EmailResponse;
import co.unicauca.bancopreguntas.email.api.dto.PageResponse;
import co.unicauca.bancopreguntas.email.application.EmailApplicationService;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;
import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada REST del registro de correos: solo el administrador los consulta. */
@RestController
@RequestMapping("/api/emails")
@Validated
@Tag(name = "Correos", description = "Registro de los correos simulados enviados")
public class EmailController {

    private final EmailApplicationService correos;

    public EmailController(EmailApplicationService correos) {
        this.correos = correos;
    }

    @GetMapping
    @Operation(summary = "Correos simulados registrados, el más reciente primero; se puede filtrar por estado. Rol ADMIN.")
    public PageResponse<EmailResponse> listar(
            AuthenticatedUser usuario,
            @RequestParam(required = false) EstadoCorreo estado,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN);
        return PageResponse.de(correos.correos(estado, page + 1, size), EmailResponse::de);
    }
}
