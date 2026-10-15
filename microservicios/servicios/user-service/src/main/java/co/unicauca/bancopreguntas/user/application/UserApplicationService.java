package co.unicauca.bancopreguntas.user.application;

import co.unicauca.bancopreguntas.user.application.port.DomainEventPublisher;
import co.unicauca.bancopreguntas.user.application.port.IdentityProvider;
import co.unicauca.bancopreguntas.user.application.port.SessionCursor;
import co.unicauca.bancopreguntas.user.application.port.UserRepository;
import co.unicauca.bancopreguntas.user.domain.EventoDeSesion;
import co.unicauca.bancopreguntas.user.domain.FiltroUsuarios;
import co.unicauca.bancopreguntas.user.domain.Pagina;
import co.unicauca.bancopreguntas.user.domain.Usuario;
import co.unicauca.bancopreguntas.user.domain.UsuarioExterno;
import co.unicauca.bancopreguntas.user.domain.event.LoginFailed;
import co.unicauca.bancopreguntas.user.domain.event.LoginSucceeded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Casos de uso del directorio de usuarios. Keycloak es la fuente de verdad: este servicio guarda una copia
 * (para listar y elegir revisores sin depender de que Keycloak esté arriba) y publica los inicios de sesión
 * que Keycloak registra, para la auditoría.
 */
@Transactional
public class UserApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(UserApplicationService.class);

    private final IdentityProvider identidad;
    private final UserRepository usuarios;
    private final SessionCursor cursor;
    private final DomainEventPublisher eventos;
    private final Clock reloj;

    public UserApplicationService(IdentityProvider identidad, UserRepository usuarios, SessionCursor cursor,
                                  DomainEventPublisher eventos, Clock reloj) {
        this.identidad = identidad;
        this.usuarios = usuarios;
        this.cursor = cursor;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /**
     * Trae los usuarios de Keycloak: crea los nuevos, actualiza nombre, correo, roles y estado de los conocidos, y
     * marca como inactivos los que Keycloak ya no devuelve. Si Keycloak responde una lista vacía y ya había
     * usuarios, se asume una falla y no se desactiva a nadie.
     */
    public ResultadoDeSincronizacion sincronizarUsuarios() {
        List<UsuarioExterno> externos = identidad.usuarios();
        Instant ahora = reloj.instant();
        if (externos.isEmpty() && usuarios.contar() > 0) {
            LOG.warn("Keycloak no devolvió ningún usuario; se conserva el directorio actual sin cambios");
            return new ResultadoDeSincronizacion(0, 0, 0);
        }
        int creados = 0;
        int actualizados = 0;
        Set<String> vigentes = new HashSet<>();
        for (UsuarioExterno externo : externos) {
            vigentes.add(externo.username());
            Optional<Usuario> conocido = usuarios.buscar(externo.username());
            if (conocido.isPresent()) {
                usuarios.guardar(conocido.get().actualizadoCon(externo, ahora));
                actualizados++;
            } else {
                usuarios.guardar(Usuario.de(externo, ahora));
                creados++;
            }
        }
        int desactivados = 0;
        for (Usuario usuario : usuarios.todos()) {
            if (usuario.activo() && !vigentes.contains(usuario.id())) {
                usuarios.guardar(usuario.inactivo(ahora));
                desactivados++;
            }
        }
        return new ResultadoDeSincronizacion(creados, actualizados, desactivados);
    }

    /**
     * Publica los inicios de sesión nuevos que registró Keycloak y actualiza el último inicio de cada usuario.
     * Solo una réplica a la vez hace este trabajo; si otra lo tiene tomado, no hace nada.
     *
     * @return cuántos eventos se publicaron
     */
    public int sincronizarSesiones() {
        Optional<Instant> desde = cursor.tomar();
        if (desde.isEmpty()) {
            return 0;
        }
        List<EventoDeSesion> nuevos = identidad.eventosDeSesion(desde.get()).stream()
                .filter(e -> e.momento().isAfter(desde.get()))
                .sorted(Comparator.comparing(EventoDeSesion::momento))
                .toList();
        for (EventoDeSesion e : nuevos) {
            if (e.exitoso()) {
                eventos.publicar(new LoginSucceeded(e.usuario(), e.ip(), e.momento()));
                usuarios.buscar(e.usuario()).ifPresent(u -> usuarios.guardar(u.conInicioDeSesion(e.momento())));
            } else {
                eventos.publicar(new LoginFailed(e.usuario(), e.ip(), e.motivo(), e.momento()));
            }
        }
        if (!nuevos.isEmpty()) {
            cursor.avanzarA(nuevos.getLast().momento());
        }
        return nuevos.size();
    }

    @Transactional(readOnly = true)
    public Pagina<Usuario> listar(FiltroUsuarios filtro, int pagina, int tamano) {
        if (tamano < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        }
        if (pagina < 1) {
            throw new IllegalArgumentException("La página debe ser al menos 1");
        }
        Pagina<Usuario> resultado = usuarios.listar(filtro, pagina, tamano);
        return pagina > resultado.totalPaginas() ? usuarios.listar(filtro, resultado.totalPaginas(), tamano) : resultado;
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> perfil(String usuarioId) {
        return usuarios.buscar(usuarioId);
    }
}
