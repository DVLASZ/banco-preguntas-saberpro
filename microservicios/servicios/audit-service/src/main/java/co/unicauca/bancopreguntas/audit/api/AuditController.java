package co.unicauca.bancopreguntas.audit.api;

import co.unicauca.bancopreguntas.audit.api.dto.EntryResponse;
import co.unicauca.bancopreguntas.audit.api.dto.PageResponse;
import co.unicauca.bancopreguntas.audit.application.AuditApplicationService;
import co.unicauca.bancopreguntas.audit.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.audit.domain.TipoEntrada;
import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Adaptador de entrada REST de la auditoría: solo el administrador la consulta. */
@RestController
@RequestMapping("/api/audit")
@Validated
@Tag(name = "Auditoría", description = "Auditoría del sistema: hechos de las preguntas, de su revisión y de la seguridad")
public class AuditController {

    private final AuditApplicationService auditoria;

    public AuditController(AuditApplicationService auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    @Operation(summary = "Auditoría completa, incluidos los inicios de sesión, con filtros opcionales. Rol ADMIN.")
    public PageResponse<EntryResponse> consultar(
            AuthenticatedUser usuario,
            @RequestParam(required = false) TipoEntrada tipo,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String preguntaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN);
        return PageResponse.de(auditoria.consultar(new FiltroAuditoria(tipo, actorId, preguntaId, desde, hasta), page + 1, size),
                EntryResponse::de);
    }
}
