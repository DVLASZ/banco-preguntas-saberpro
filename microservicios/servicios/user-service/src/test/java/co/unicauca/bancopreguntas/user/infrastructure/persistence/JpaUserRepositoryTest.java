package co.unicauca.bancopreguntas.user.infrastructure.persistence;

import co.unicauca.bancopreguntas.user.EmbeddedPostgresConfig;
import co.unicauca.bancopreguntas.user.domain.FiltroUsuarios;
import co.unicauca.bancopreguntas.user.domain.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de los adaptadores de persistencia contra un PostgreSQL real embebido (sin Docker): aplica las
 * migraciones, valida el mapeo y comprueba el cerrojo entre réplicas del cursor de sesiones.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaUserRepository.class, JpaSessionCursor.class, EmbeddedPostgresConfig.class})
@DisplayName("Persistencia de usuarios (PostgreSQL)")
class JpaUserRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private JpaUserRepository repositorio;

    @Autowired
    private JpaSessionCursor cursor;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager transacciones;

    private static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario guardar(String id, String nombre, String correo, boolean activo, String... roles) {
        Usuario u = new Usuario(id, "kc-" + id, nombre, correo, Set.of(roles), activo, null, T0);
        repositorio.guardar(u);
        em.clear();
        return u;
    }

    @Test
    @DisplayName("guarda y recupera un usuario con sus roles, correo y último inicio de sesión")
    void guardaYRecupera() {
        String id = "ana-" + sufijo();
        Usuario original = guardar(id, "Ana Pérez", "ana@correo.test", true, "AUTHOR", "REVIEWER");
        repositorio.guardar(original.conInicioDeSesion(T0.plusSeconds(30)));
        em.clear();

        Usuario leido = repositorio.buscar(id).orElseThrow();

        assertThat(leido.nombre()).isEqualTo("Ana Pérez");
        assertThat(leido.correo()).isEqualTo("ana@correo.test");
        assertThat(leido.roles()).containsExactlyInAnyOrder("AUTHOR", "REVIEWER");
        assertThat(leido.activo()).isTrue();
        assertThat(leido.ultimoInicioDeSesion()).isEqualTo(T0.plusSeconds(30));
        assertThat(leido.keycloakId()).isEqualTo("kc-" + id);
        assertThat(repositorio.buscar("no-existe-" + sufijo())).isEmpty();
    }

    @Test
    @DisplayName("un usuario sin roles se guarda y se lee sin roles")
    void sinRoles() {
        String id = "sinroles-" + sufijo();
        guardar(id, "Sin Roles", null, true);

        assertThat(repositorio.buscar(id).orElseThrow().roles()).isEmpty();
    }

    @Test
    @DisplayName("guardar de nuevo actualiza la misma fila")
    void actualiza() {
        String id = "ana-" + sufijo();
        Usuario u = guardar(id, "Ana", null, true, "AUTHOR");

        repositorio.guardar(u.inactivo(T0.plusSeconds(5)));
        em.clear();

        assertThat(repositorio.buscar(id).orElseThrow().activo()).isFalse();
        assertThat(repositorio.todos()).extracting(Usuario::id).contains(id);
    }

    @Test
    @DisplayName("lista los revisores activos, sin confundir roles de nombre parecido")
    void filtroPorRol() {
        String s = sufijo();
        guardar("rev1-" + s, "Zeta " + s, null, true, "REVIEWER");
        guardar("rev2-" + s, "Alfa " + s, null, true, "REVIEWER", "AUTHOR");
        guardar("rev3-" + s, "Inactivo " + s, null, false, "REVIEWER");
        guardar("aut-" + s, "Autor " + s, null, true, "AUTHOR");
        guardar("meta-" + s, "Meta " + s, null, true, "META_REVIEWER");

        var activos = repositorio.listar(new FiltroUsuarios("REVIEWER", s, true), 1, 50);
        var todos = repositorio.listar(new FiltroUsuarios("REVIEWER", s, false), 1, 50);

        assertThat(activos.elementos()).extracting(Usuario::nombre).containsExactly("Alfa " + s, "Zeta " + s); // por nombre
        assertThat(todos.totalElementos()).isEqualTo(3);
    }

    @Test
    @DisplayName("el texto busca en usuario, nombre y correo sin distinguir mayúsculas")
    void filtroPorTexto() {
        String s = sufijo();
        guardar("juan-" + s, "Juan Gómez", "jgomez@uni.test", true, "AUTHOR");
        guardar("otra-" + s, "Otra Persona", "otra@" + s + ".test", true, "AUTHOR");

        assertThat(repositorio.listar(new FiltroUsuarios(null, "JUAN-" + s, true), 1, 50).elementos())
                .extracting(Usuario::id).containsExactly("juan-" + s);
        assertThat(repositorio.listar(new FiltroUsuarios(null, "gómez", true), 1, 50).elementos())
                .extracting(Usuario::id).contains("juan-" + s);
        assertThat(repositorio.listar(new FiltroUsuarios(null, "@" + s + ".TEST", true), 1, 50).elementos())
                .extracting(Usuario::id).containsExactly("otra-" + s);
    }

    @Test
    @DisplayName("los comodines del texto se tratan como texto normal")
    void comodines() {
        String s = sufijo();
        guardar("pct-" + s, "Cien%" + s, null, true, "AUTHOR");
        guardar("otro-" + s, "Cienx" + s, null, true, "AUTHOR");

        assertThat(repositorio.listar(new FiltroUsuarios(null, "cien%" + s, true), 1, 50).elementos())
                .extracting(Usuario::id).containsExactly("pct-" + s);
        assertThat(repositorio.listar(new FiltroUsuarios(null, "%", true), 1, 50).elementos())
                .extracting(Usuario::id).contains("pct-" + s).doesNotContain("otro-" + s);
    }

    @Test
    @DisplayName("la paginación respeta página, tamaño y total")
    void paginacion() {
        String s = sufijo();
        for (int i = 0; i < 5; i++) {
            guardar("p" + i + "-" + s, "Persona " + i + " " + s, null, true, "AUTHOR");
        }

        var segunda = repositorio.listar(new FiltroUsuarios(null, s, true), 2, 2);

        assertThat(segunda.elementos()).hasSize(2);
        assertThat(segunda.totalElementos()).isEqualTo(5);
        assertThat(segunda.totalPaginas()).isEqualTo(3);
        assertThat(repositorio.contar()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("el cursor arranca con una fecha y se puede adelantar")
    void cursor() {
        Instant inicial = cursor.tomar().orElseThrow();
        Instant nuevo = inicial.plusSeconds(120);

        cursor.avanzarA(nuevo);
        em.flush();
        em.clear();

        assertThat(cursor.tomar()).contains(nuevo);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("si una réplica tiene el cursor, otra no lo toma ni espera")
    void cerrojoEntreReplicas() {
        TransactionTemplate primera = new TransactionTemplate(transacciones);
        TransactionTemplate segunda = new TransactionTemplate(transacciones);
        segunda.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Optional<Instant> deLaOtra = primera.execute(estado -> {
            Optional<Instant> mio = cursor.tomar();
            assertThat(mio).isPresent();
            // mientras esta transacción lo tiene, otra transacción (otra réplica) no lo obtiene
            return CompletableFuture.supplyAsync(() -> segunda.execute(e -> cursor.tomar())).join();
        });

        assertThat(deLaOtra).isEmpty();
        // liberado el cerrojo, cualquiera puede tomarlo de nuevo
        Optional<Instant> liberado = segunda.execute(e -> cursor.tomar());
        assertThat(liberado).isPresent();
    }
}
