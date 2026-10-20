package co.unicauca.bancopreguntas.simulation.api;

import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import co.unicauca.bancopreguntas.simulation.api.dto.AnswerRequest;
import co.unicauca.bancopreguntas.simulation.api.dto.CatalogSummaryResponse;
import co.unicauca.bancopreguntas.simulation.api.dto.CreateSimulationRequest;
import co.unicauca.bancopreguntas.simulation.api.dto.PageResponse;
import co.unicauca.bancopreguntas.simulation.api.dto.SimulationResponse;
import co.unicauca.bancopreguntas.simulation.api.dto.SimulationSummaryResponse;
import co.unicauca.bancopreguntas.simulation.application.SimulationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST de los simulacros (RF-21 a RF-24). El estudiante los genera y los presenta; docentes y
 * administradores pueden consultarlos. Cada operación exige su rol (autorización redundante con el gateway).
 */
@RestController
@RequestMapping("/api/simulations")
@Validated
@Tag(name = "Simulacros", description = "Generación, presentación y calificación automática de simulacros")
public class SimulationController {

    private final SimulationApplicationService simulacros;

    public SimulationController(SimulationApplicationService simulacros) {
        this.simulacros = simulacros;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generar un simulacro aleatorio con preguntas publicadas y empezar a contar el tiempo. Rol STUDENT.",
            description = "Se puede filtrar por competencias, temas y dificultades. Si no hay suficientes preguntas "
                    + "responde 409 con cuántas hay.")
    public SimulationResponse crear(AuthenticatedUser usuario, @Valid @RequestBody CreateSimulationRequest solicitud) {
        usuario.requireAnyRole(Roles.STUDENT);
        return SimulationResponse.de(simulacros.iniciar(usuario.id(), solicitud.aCriterios()));
    }

    @GetMapping
    @Operation(summary = "Mis simulacros, el más reciente primero. Rol STUDENT.")
    public PageResponse<SimulationSummaryResponse> mios(
            AuthenticatedUser usuario,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.STUDENT);
        return PageResponse.de(simulacros.misSimulacros(usuario.id(), page + 1, size), SimulationSummaryResponse::de);
    }

    @GetMapping("/catalog/summary")
    @Operation(summary = "Cuántas preguntas publicadas hay para simulacros, por competencia y dificultad. "
            + "Roles STUDENT, TEACHER o ADMIN.")
    public CatalogSummaryResponse catalogo(AuthenticatedUser usuario) {
        usuario.requireAnyRole(Roles.STUDENT, Roles.TEACHER, Roles.ADMIN);
        return CatalogSummaryResponse.de(simulacros.resumenDelCatalogo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un simulacro y el tiempo que le queda. El estudiante ve los suyos; TEACHER y ADMIN, "
            + "cualquiera.",
            description = "Mientras está en curso no se revela la respuesta correcta. Si el tiempo ya se agotó, se "
                    + "califica en ese momento.")
    public SimulationResponse obtener(AuthenticatedUser usuario, @PathVariable String id) {
        usuario.requireAnyRole(Roles.STUDENT, Roles.TEACHER, Roles.ADMIN);
        boolean puedeVerTodos = usuario.hasRole(Roles.TEACHER) || usuario.hasRole(Roles.ADMIN);
        return SimulationResponse.de(simulacros.obtener(id, usuario.id(), puedeVerTodos));
    }

    @PostMapping("/{id}/answers")
    @Operation(summary = "Responder una pregunta del simulacro (se puede cambiar antes de finalizar). Rol STUDENT.",
            description = "409 si el simulacro ya terminó o si el tiempo se agotó (en ese caso se califica con lo contestado).")
    public SimulationResponse responder(AuthenticatedUser usuario, @PathVariable String id,
                                        @Valid @RequestBody AnswerRequest respuesta) {
        usuario.requireAnyRole(Roles.STUDENT);
        return SimulationResponse.de(simulacros.responder(id, usuario.id(), respuesta.preguntaId(),
                Character.toUpperCase(respuesta.opcion().charAt(0))));
    }

    @PostMapping("/{id}/finish")
    @Operation(summary = "Finalizar el simulacro: se califica automáticamente y se muestran las respuestas correctas. "
            + "Rol STUDENT.")
    public SimulationResponse finalizar(AuthenticatedUser usuario, @PathVariable String id) {
        usuario.requireAnyRole(Roles.STUDENT);
        return SimulationResponse.de(simulacros.finalizar(id, usuario.id()));
    }
}
