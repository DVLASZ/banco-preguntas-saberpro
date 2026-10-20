package co.unicauca.bancopreguntas.user.api;

import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.Roles;
import co.unicauca.bancopreguntas.user.api.dto.MeResponse;
import co.unicauca.bancopreguntas.user.api.dto.PageResponse;
import co.unicauca.bancopreguntas.user.api.dto.SyncResponse;
import co.unicauca.bancopreguntas.user.api.dto.UserResponse;
import co.unicauca.bancopreguntas.user.application.UserApplicationService;
import co.unicauca.bancopreguntas.user.domain.FiltroUsuarios;
import co.unicauca.bancopreguntas.user.domain.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.TreeSet;

/** Adaptador de entrada REST del directorio de usuarios. */
@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Usuarios", description = "Directorio de usuarios sincronizado con Keycloak")
public class UserController {

    private final UserApplicationService usuarios;

    public UserController(UserApplicationService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios, por ejemplo los revisores disponibles. Rol ADMIN.",
            description = "rol=REVIEWER lista los candidatos a revisor. texto busca en usuario, nombre y correo sin "
                    + "distinguir mayúsculas. Por defecto solo trae cuentas activas.")
    public PageResponse<UserResponse> listar(
            AuthenticatedUser usuario,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "true") boolean soloActivos,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page debe ser 0 o mayor") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {
        usuario.requireAnyRole(Roles.ADMIN);
        return PageResponse.de(usuarios.listar(new FiltroUsuarios(rol, texto, soloActivos), page + 1, size),
                UserResponse::de);
    }

    @GetMapping("/me")
    @Operation(summary = "Quién soy: mi usuario, roles y perfil. Cualquier rol del sistema.")
    public MeResponse yo(AuthenticatedUser usuario) {
        usuario.requireAnyRole(Roles.ADMIN, Roles.AUTHOR, Roles.REVIEWER, Roles.TEACHER, Roles.STUDENT);
        List<String> roles = List.copyOf(new TreeSet<>(usuario.roles()));
        return usuarios.perfil(usuario.id())
                .map(u -> new MeResponse(u.id(), u.nombre(), u.correo(), roles, true, u.ultimoInicioDeSesion()))
                .orElseGet(() -> new MeResponse(usuario.id(), usuario.id(), null, roles, false, null));
    }

    @PostMapping("/sync")
    @Operation(summary = "Sincronizar ahora con Keycloak: usuarios nuevos y inicios de sesión. Rol ADMIN.",
            description = "Normalmente se hace solo cada pocos segundos; esto fuerza una vuelta.")
    public SyncResponse sincronizar(AuthenticatedUser usuario) {
        usuario.requireAnyRole(Roles.ADMIN);
        return SyncResponse.de(usuarios.sincronizarUsuarios(), usuarios.sincronizarSesiones());
    }
}
