package co.unicauca.bancopreguntas.tracking.api;

import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import co.unicauca.bancopreguntas.tracking.api.dto.GroupReportResponse;
import co.unicauca.bancopreguntas.tracking.api.dto.HistoryEntryResponse;
import co.unicauca.bancopreguntas.tracking.api.dto.PageResponse;
import co.unicauca.bancopreguntas.tracking.api.dto.StatsResponse;
import co.unicauca.bancopreguntas.tracking.api.dto.StrengthsResponse;
import co.unicauca.bancopreguntas.tracking.application.TrackingApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Adaptador de entrada REST del seguimiento académico (RF-25 a RF-28). Cada estudiante ve lo suyo; docentes y
 * administradores ven el de cualquier estudiante y el reporte del grupo. Cada operación exige su rol.
 */
@RestController
@RequestMapping("/api/tracking")
@Validated
@Tag(name = "Seguimiento académico", description = "Historial, estadísticas, fortalezas y debilidades, y reportes de grupo")
public class TrackingController {

    private final TrackingApplicationService seguimiento;

    public TrackingController(TrackingApplicationService seguimiento) {
        this.seguimiento = seguimiento;
    }

    // ------------------------------------------------------------------ el estudiante

    @GetMapping("/me/history")
    @Operation(summary = "Mi historial de simulacros, el más reciente primero (RF-25). Rol STUDENT.")
    public PageResponse<HistoryEntryResponse> miHistorial(
            AuthenticatedUser usuario,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.STUDENT);
        return PageResponse.de(seguimiento.historial(usuario.id(), page + 1, size), HistoryEntryResponse::de);
    }

    @GetMapping("/me/stats")
    @Operation(summary = "Mis estadísticas individuales (RF-26). Rol STUDENT.")
    public StatsResponse misEstadisticas(AuthenticatedUser usuario) {
        usuario.requireAnyRole(Roles.STUDENT);
        return StatsResponse.de(seguimiento.estadisticas(usuario.id()));
    }

    @GetMapping("/me/competencies")
    @Operation(summary = "Mis fortalezas y debilidades por competencia (RF-27). Rol STUDENT.")
    public StrengthsResponse misCompetencias(AuthenticatedUser usuario) {
        usuario.requireAnyRole(Roles.STUDENT);
        return StrengthsResponse.de(seguimiento.competencias(usuario.id()), seguimiento.umbrales());
    }

    // ------------------------------------------------------------------ docentes y coordinadores

    @GetMapping("/students/{estudianteId}/history")
    @Operation(summary = "Historial de simulacros de un estudiante. Roles TEACHER o ADMIN.")
    public PageResponse<HistoryEntryResponse> historialDe(
            AuthenticatedUser usuario, @PathVariable String estudianteId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.TEACHER, Roles.ADMIN);
        return PageResponse.de(seguimiento.historial(estudianteId, page + 1, size), HistoryEntryResponse::de);
    }

    @GetMapping("/students/{estudianteId}/stats")
    @Operation(summary = "Estadísticas individuales de un estudiante. Roles TEACHER o ADMIN.")
    public StatsResponse estadisticasDe(AuthenticatedUser usuario, @PathVariable String estudianteId) {
        usuario.requireAnyRole(Roles.TEACHER, Roles.ADMIN);
        return StatsResponse.de(seguimiento.estadisticas(estudianteId));
    }

    @GetMapping("/students/{estudianteId}/competencies")
    @Operation(summary = "Fortalezas y debilidades de un estudiante por competencia. Roles TEACHER o ADMIN.")
    public StrengthsResponse competenciasDe(AuthenticatedUser usuario, @PathVariable String estudianteId) {
        usuario.requireAnyRole(Roles.TEACHER, Roles.ADMIN);
        return StrengthsResponse.de(seguimiento.competencias(estudianteId), seguimiento.umbrales());
    }

    @GetMapping("/reports/group")
    @Operation(summary = "Reporte del grupo: promedio general, desempeño por competencia y una fila por estudiante "
            + "(RF-28). Roles TEACHER o ADMIN.")
    public GroupReportResponse reporteDeGrupo(AuthenticatedUser usuario) {
        usuario.requireAnyRole(Roles.TEACHER, Roles.ADMIN);
        return GroupReportResponse.de(seguimiento.reporteDeGrupo());
    }

    @GetMapping(value = "/reports/group.csv", produces = "text/csv")
    @Operation(summary = "El detalle del reporte de grupo en CSV, para abrirlo en una hoja de cálculo (RF-28). "
            + "Roles TEACHER o ADMIN.")
    public ResponseEntity<byte[]> reporteDeGrupoCsv(AuthenticatedUser usuario) {
        usuario.requireAnyRole(Roles.TEACHER, Roles.ADMIN);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-de-grupo.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(seguimiento.reporteDeGrupo().aCsv().getBytes(StandardCharsets.UTF_8));
    }
}
