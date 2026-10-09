package co.unicauca.bancopreguntas.question.api;

import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.ForbiddenException;
import co.unicauca.bancopreguntas.platform.security.Roles;
import co.unicauca.bancopreguntas.question.api.dto.QuestionPageResponse;
import co.unicauca.bancopreguntas.question.api.dto.QuestionRequest;
import co.unicauca.bancopreguntas.question.api.dto.QuestionResponse;
import co.unicauca.bancopreguntas.question.application.QuestionApplicationService;
import co.unicauca.bancopreguntas.question.domain.Competencia;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.FiltroPreguntas;
import co.unicauca.bancopreguntas.question.domain.Question;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Adaptador de entrada REST de la gestión de preguntas (HU-01, HU-02, HU-03, consulta de HU-04 y
 * publicación).
 *
 * <p>La identidad y los roles llegan en un {@link AuthenticatedUser}. El gateway valida el token y propaga
 * la identidad, y este servicio la comprueba otra vez (autorización redundante): cada operación exige su
 * rol, y las que modifican una pregunta aplican además la regla de dominio de que solo la toca su autor.
 */
@RestController
@RequestMapping("/api/questions")
@Validated
@Tag(name = "Preguntas", description = "Creación, consulta y ciclo de vida de las preguntas del banco")
public class QuestionController {

    private final QuestionApplicationService preguntas;

    public QuestionController(QuestionApplicationService preguntas) {
        this.preguntas = preguntas;
    }

    @PostMapping
    @Operation(summary = "Crear una pregunta en estado Borrador (HU-01). Rol AUTHOR.")
    public ResponseEntity<QuestionResponse> crear(AuthenticatedUser usuario,
                                                  @Valid @RequestBody QuestionRequest solicitud) {
        usuario.requireAnyRole(Roles.AUTHOR);
        Question creada = preguntas.crearBorrador(solicitud.aContenido(), usuario.id());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(QuestionResponse.de(creada));
    }

    @GetMapping
    @Operation(summary = "Listar mis preguntas con filtros y paginación (HU-03). Rol AUTHOR.",
            description = "Devuelve solo las preguntas del usuario autenticado. "
                    + "El texto se busca sin distinguir mayúsculas ni tildes en el nombre, la pregunta, el tema y el subtema.")
    public QuestionPageResponse listarMias(AuthenticatedUser usuario,
                                           @RequestParam(required = false) EstadoPregunta estado,
                                           @RequestParam(required = false) Competencia competencia,
                                           @RequestParam(required = false) String texto,
                                           @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
                                           @RequestParam(defaultValue = "10")
                                           @Min(value = 1, message = "size debe ser al menos 1")
                                           @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.AUTHOR);
        FiltroPreguntas filtro = new FiltroPreguntas(estado, competencia, texto);
        return QuestionPageResponse.de(preguntas.buscarDelAutor(usuario.id(), filtro, page + 1, size));
    }

    @GetMapping("/all")
    @Operation(summary = "Buscar preguntas de todos los autores (por ejemplo, las aprobadas que esperan publicación). Rol ADMIN.")
    public QuestionPageResponse listarTodas(AuthenticatedUser usuario,
                                            @RequestParam(required = false) String autor,
                                            @RequestParam(required = false) EstadoPregunta estado,
                                            @RequestParam(required = false) Competencia competencia,
                                            @RequestParam(required = false) String texto,
                                            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
                                            @RequestParam(defaultValue = "10")
                                            @Min(value = 1, message = "size debe ser al menos 1")
                                            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN);
        return QuestionPageResponse.de(preguntas.buscarTodas(autor,
                new FiltroPreguntas(estado, competencia, texto), page + 1, size));
    }

    @GetMapping("/pending-review")
    @Operation(summary = "Preguntas pendientes de revisión (HU-04). Rol ADMIN.")
    public QuestionPageResponse listarPendientesDeRevision(
            AuthenticatedUser usuario,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN);
        return QuestionPageResponse.de(preguntas.buscarPendientesDeRevision(page + 1, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una pregunta con su historial de estados. ADMIN y REVIEWER ven cualquiera; AUTHOR, solo las suyas.")
    public QuestionResponse obtener(AuthenticatedUser usuario, @PathVariable String id) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.REVIEWER, Roles.AUTHOR);
        Question pregunta = preguntas.obtener(id);
        boolean puedeVerTodas = usuario.hasRole(Roles.ADMIN) || usuario.hasRole(Roles.REVIEWER);
        if (!puedeVerTodas && !pregunta.getAutor().equals(usuario.id())) {
            throw new ForbiddenException("Solo el autor de la pregunta puede consultarla");
        }
        return QuestionResponse.de(pregunta, preguntas.historial(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar una pregunta; solo su autor y solo en estado Borrador (RF-06). Rol AUTHOR.")
    public QuestionResponse modificar(AuthenticatedUser usuario,
                                      @PathVariable String id,
                                      @Valid @RequestBody QuestionRequest solicitud) {
        usuario.requireAnyRole(Roles.AUTHOR);
        return QuestionResponse.de(preguntas.actualizarContenido(id, solicitud.aContenido(), usuario.id()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Enviar a revisión: de Borrador a Pendiente de revisión (HU-02). Rol AUTHOR.",
            description = "Aplica de nuevo la validación estructural y publica el evento question.submitted.")
    public QuestionResponse enviarARevision(AuthenticatedUser usuario, @PathVariable String id) {
        usuario.requireAnyRole(Roles.AUTHOR);
        return QuestionResponse.de(preguntas.enviarARevision(id, usuario.id()));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reabrir una pregunta rechazada para corregirla: vuelve a Borrador como una versión nueva. Rol AUTHOR.",
            description = "Publica question.reopened, con el que el servicio de revisión cierra la ronda abierta.")
    public QuestionResponse reabrir(AuthenticatedUser usuario, @PathVariable String id) {
        usuario.requireAnyRole(Roles.AUTHOR);
        return QuestionResponse.de(preguntas.reabrirParaCorregir(id, usuario.id()));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publicar una pregunta aprobada: de Aprobada a Publicada. Rol ADMIN.",
            description = "Publica el evento question.published.")
    public QuestionResponse publicar(AuthenticatedUser usuario, @PathVariable String id) {
        usuario.requireAnyRole(Roles.ADMIN);
        return QuestionResponse.de(preguntas.publicar(id, usuario.id()));
    }
}
