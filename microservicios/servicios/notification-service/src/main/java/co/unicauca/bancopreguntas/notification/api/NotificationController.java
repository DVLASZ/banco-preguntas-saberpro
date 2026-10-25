package co.unicauca.bancopreguntas.notification.api;

import co.unicauca.bancopreguntas.notification.api.dto.EmailResponse;
import co.unicauca.bancopreguntas.notification.api.dto.MarkedReadResponse;
import co.unicauca.bancopreguntas.notification.api.dto.NotificationResponse;
import co.unicauca.bancopreguntas.notification.api.dto.PageResponse;
import co.unicauca.bancopreguntas.notification.api.dto.UnreadCountResponse;
import co.unicauca.bancopreguntas.notification.application.NotificationApplicationService;
import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST de las notificaciones. Cada usuario ve solo las suyas; los correos simulados los
 * consulta el administrador.
 */
@RestController
@RequestMapping("/api/notifications")
@Validated
@Tag(name = "Notificaciones", description = "Bandeja de notificaciones y correos simulados")
public class NotificationController {

    private final NotificationApplicationService notificaciones;

    public NotificationController(NotificationApplicationService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping
    @Operation(summary = "Mi bandeja de notificaciones, la más reciente primero. ADMIN, AUTHOR o REVIEWER.")
    public PageResponse<NotificationResponse> mias(
            AuthenticatedUser usuario,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.AUTHOR, Roles.REVIEWER);
        return PageResponse.de(notificaciones.bandeja(usuario.id(), soloNoLeidas, page + 1, size),
                NotificationResponse::de);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Cuántas notificaciones tengo sin leer.")
    public UnreadCountResponse sinLeer(AuthenticatedUser usuario) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.AUTHOR, Roles.REVIEWER);
        return new UnreadCountResponse(notificaciones.noLeidas(usuario.id()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Marcar una notificación mía como leída.")
    public NotificationResponse marcarLeida(AuthenticatedUser usuario, @PathVariable String id) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.AUTHOR, Roles.REVIEWER);
        return NotificationResponse.de(notificaciones.marcarComoLeida(id, usuario.id()));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Marcar todas mis notificaciones como leídas.")
    public MarkedReadResponse marcarTodasLeidas(AuthenticatedUser usuario) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.AUTHOR, Roles.REVIEWER);
        return new MarkedReadResponse(notificaciones.marcarTodasComoLeidas(usuario.id()));
    }

    @GetMapping("/emails")
    @Operation(summary = "Correos simulados enviados, el más reciente primero. Rol ADMIN.")
    public PageResponse<EmailResponse> correos(
            AuthenticatedUser usuario,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN);
        return PageResponse.de(notificaciones.correosEnviados(page + 1, size), EmailResponse::de);
    }
}
