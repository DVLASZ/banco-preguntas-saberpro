package co.unicauca.bancopreguntas.history.api;

import co.unicauca.bancopreguntas.history.api.dto.EntryResponse;
import co.unicauca.bancopreguntas.history.api.dto.PageResponse;
import co.unicauca.bancopreguntas.history.application.HistoryApplicationService;
import co.unicauca.bancopreguntas.history.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.history.domain.Observador;
import co.unicauca.bancopreguntas.history.domain.TipoEntrada;
import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Adaptador de entrada REST del historial. Cada rol ve una parte distinta: el autor, el rastro de sus
 * preguntas; el revisor, el de las preguntas que revisa o revisó; el administrador, todo y la auditoría.
 */
@RestController
@RequestMapping("/api/history")
@Validated
@Tag(name = "Historial", description = "Historial de preguntas y auditoría del sistema")
public class HistoryController {

    private final HistoryApplicationService historial;

    public HistoryController(HistoryApplicationService historial) {
        this.historial = historial;
    }

    private static Observador observador(AuthenticatedUser usuario) {
        return new Observador(usuario.id(), usuario.hasRole(Roles.ADMIN), usuario.hasRole(Roles.AUTHOR),
                usuario.hasRole(Roles.REVIEWER));
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "El rastro de una pregunta en orden cronológico. ADMIN, AUTHOR (sus preguntas) o REVIEWER (las que revisa).",
            description = "El autor ve el rastro de su pregunta y los resultados de cada ronda; el revisor y el "
                    + "administrador ven además las evaluaciones y asignaciones.")
    public List<EntryResponse> deUnaPregunta(AuthenticatedUser usuario, @PathVariable String questionId) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.AUTHOR, Roles.REVIEWER);
        return historial.lineaDeTiempo(questionId, observador(usuario)).stream().map(EntryResponse::de).toList();
    }

    @GetMapping("/me")
    @Operation(summary = "Todo lo que puedo ver de mis preguntas, lo más reciente primero. ADMIN, AUTHOR o REVIEWER.")
    public PageResponse<EntryResponse> visibles(
            AuthenticatedUser usuario,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.AUTHOR, Roles.REVIEWER);
        return PageResponse.de(historial.visiblesPara(observador(usuario), page + 1, size), EntryResponse::de);
    }

    @GetMapping("/audit")
    @Operation(summary = "Auditoría completa, incluidos los inicios de sesión, con filtros opcionales. Rol ADMIN.")
    public PageResponse<EntryResponse> auditoria(
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
        return PageResponse.de(historial.auditoria(new FiltroAuditoria(tipo, actorId, preguntaId, desde, hasta),
                page + 1, size), EntryResponse::de);
    }
}
