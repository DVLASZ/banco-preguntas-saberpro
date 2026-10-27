package co.unicauca.bancopreguntas.lifecycle.api;

import co.unicauca.bancopreguntas.lifecycle.api.dto.CambioEstadoResponse;
import co.unicauca.bancopreguntas.lifecycle.api.dto.EnvioRequest;
import co.unicauca.bancopreguntas.lifecycle.api.dto.LifecycleResponse;
import co.unicauca.bancopreguntas.lifecycle.api.dto.PublicacionRequest;
import co.unicauca.bancopreguntas.lifecycle.api.dto.RegistroRequest;
import co.unicauca.bancopreguntas.lifecycle.application.LifecycleApplicationService;
import co.unicauca.bancopreguntas.lifecycle.domain.CicloDeVida;
import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.ForbiddenException;
import co.unicauca.bancopreguntas.platform.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST del ciclo de vida de las preguntas. Lo llama el servicio de preguntas (el gateway no lo
 * expone) pasando la identidad de quien hizo la petición; cada operación vuelve a exigir su rol (autorización
 * redundante) y las que tocan una pregunta aplican además la regla de que solo la mueve su autor.
 */
@RestController
@RequestMapping("/api/lifecycle/questions")
@Validated
@Tag(name = "Ciclo de vida", description = "Estados de las preguntas, transiciones e historial")
public class LifecycleController {

    private final LifecycleApplicationService ciclos;

    public LifecycleController(LifecycleApplicationService ciclos) {
        this.ciclos = ciclos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar una pregunta recién creada en estado Borrador. Rol AUTHOR.")
    public LifecycleResponse registrar(AuthenticatedUser usuario, @Valid @RequestBody RegistroRequest solicitud) {
        usuario.requireAnyRole(Roles.AUTHOR);
        return LifecycleResponse.de(ciclos.registrar(solicitud.preguntaId(), usuario.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Estado actual de una pregunta. ADMIN y REVIEWER ven cualquiera; AUTHOR, solo las suyas.")
    public LifecycleResponse obtener(AuthenticatedUser usuario, @PathVariable String id) {
        return LifecycleResponse.de(visible(usuario, id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historial de estados de una pregunta, del más antiguo al más reciente.")
    public List<CambioEstadoResponse> historial(AuthenticatedUser usuario, @PathVariable String id) {
        visible(usuario, id);
        return ciclos.historial(id).stream().map(CambioEstadoResponse::de).toList();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Enviar a revisión: de Borrador a Pendiente de revisión (HU-02). Rol AUTHOR.",
            description = "Publica question.state-changed y question.submitted.")
    public LifecycleResponse enviarARevision(AuthenticatedUser usuario, @PathVariable String id,
                                             @Valid @RequestBody EnvioRequest solicitud) {
        usuario.requireAnyRole(Roles.AUTHOR);
        return LifecycleResponse.de(ciclos.enviarARevision(id, usuario.id(), solicitud.titulo(), solicitud.competencia()));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reabrir una pregunta rechazada: vuelve a Borrador como una versión nueva. Rol AUTHOR.",
            description = "Publica question.state-changed y question.reopened.")
    public LifecycleResponse reabrir(AuthenticatedUser usuario, @PathVariable String id) {
        usuario.requireAnyRole(Roles.AUTHOR);
        return LifecycleResponse.de(ciclos.reabrir(id, usuario.id()));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publicar una pregunta aprobada: de Aprobada a Publicada. Rol ADMIN.",
            description = "Recibe el contenido de la pregunta y publica question.state-changed y question.published.")
    public LifecycleResponse publicar(AuthenticatedUser usuario, @PathVariable String id,
                                      @Valid @RequestBody PublicacionRequest solicitud) {
        usuario.requireAnyRole(Roles.ADMIN);
        return LifecycleResponse.de(ciclos.publicar(id, usuario.id(), solicitud.aContenido()));
    }

    /** El ciclo de vida, si quien pregunta puede verlo: ADMIN y REVIEWER ven todos y el AUTHOR solo los suyos. */
    private CicloDeVida visible(AuthenticatedUser usuario, String id) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.REVIEWER, Roles.AUTHOR);
        CicloDeVida ciclo = ciclos.obtener(id);
        boolean puedeVerTodas = usuario.hasRole(Roles.ADMIN) || usuario.hasRole(Roles.REVIEWER);
        if (!puedeVerTodas && !ciclo.getAutorId().equals(usuario.id())) {
            throw new ForbiddenException("Solo el autor de la pregunta puede consultarla");
        }
        return ciclo;
    }
}
