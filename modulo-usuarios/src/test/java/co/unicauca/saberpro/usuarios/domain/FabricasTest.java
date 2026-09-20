package co.unicauca.saberpro.usuarios.domain;

import co.unicauca.saberpro.usuarios.domain.access.SqliteUserRepository;
import co.unicauca.saberpro.usuarios.domain.access.UserRepositoryFactory;
import co.unicauca.saberpro.usuarios.domain.security.Argon2PasswordHasher;
import co.unicauca.saberpro.usuarios.domain.security.PasswordHasherFactory;
import co.unicauca.saberpro.usuarios.domain.validation.DefaultPasswordPolicy;
import co.unicauca.saberpro.usuarios.domain.validation.PasswordPolicyFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Las tres fábricas (Singleton + Factory Method) que entregan las implementaciones por defecto. */
class FabricasTest {

    private static final Path BASE_DE_DATOS = Path.of("saberpro.db");

    /** Crear el repositorio por defecto abre el archivo saberpro.db: si no estaba antes, se borra al terminar la JVM. */
    @BeforeAll
    static void programarLimpiezaDeLaBaseDeDatos() {
        if (!Files.exists(BASE_DE_DATOS)) {
            BASE_DE_DATOS.toFile().deleteOnExit();
        }
    }

    @Test
    void cadaFabrica_esUnSingleton() {
        assertSame(UserRepositoryFactory.getInstance(), UserRepositoryFactory.getInstance());
        assertSame(PasswordHasherFactory.getInstance(), PasswordHasherFactory.getInstance());
        assertSame(PasswordPolicyFactory.getInstance(), PasswordPolicyFactory.getInstance());
    }

    @Test
    void fabricaDeRepositorio_entregaSqliteParaDefaultYSqlite() {
        assertInstanceOf(SqliteUserRepository.class, UserRepositoryFactory.getInstance().getRepository("default"));
        assertInstanceOf(SqliteUserRepository.class, UserRepositoryFactory.getInstance().getRepository("sqlite"));
    }

    @Test
    void fabricaDeHasher_entregaArgon2ParaDefaultYArgon2() {
        assertInstanceOf(Argon2PasswordHasher.class, PasswordHasherFactory.getInstance().getHasher("default"));
        assertInstanceOf(Argon2PasswordHasher.class, PasswordHasherFactory.getInstance().getHasher("argon2"));
    }

    @Test
    void fabricaDePolitica_entregaLaPoliticaPorDefecto() {
        assertInstanceOf(DefaultPasswordPolicy.class, PasswordPolicyFactory.getInstance().getPolicy("default"));
    }

    @Test
    void tipoDesconocido_devuelveNulo() {
        assertNull(UserRepositoryFactory.getInstance().getRepository("mongo"));
        assertNull(PasswordHasherFactory.getInstance().getHasher("md5"));
        assertNull(PasswordPolicyFactory.getInstance().getPolicy("estricta"));
    }
}
