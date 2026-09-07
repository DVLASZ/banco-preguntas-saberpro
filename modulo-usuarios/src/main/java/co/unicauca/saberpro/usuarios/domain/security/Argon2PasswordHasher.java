package co.unicauca.saberpro.usuarios.domain.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/**
 * Implementacion de {@link IPasswordHasher} basada en Argon2id (libreria
 * argon2-jvm), el algoritmo recomendado actualmente para almacenar
 * contrasenas. El hash resultante incluye la sal y los parametros del
 * algoritmo, por lo que no es necesario almacenarlos por separado.
 */
public class Argon2PasswordHasher implements IPasswordHasher {

    private static final int ITERATIONS = 3;
    private static final int MEMORY_KB = 65536; // 64 MB
    private static final int PARALLELISM = 1;

    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    @Override
    public String hash(String plainPassword) {
        char[] chars = plainPassword.toCharArray();
        try {
            return argon2.hash(ITERATIONS, MEMORY_KB, PARALLELISM, chars);
        } finally {
            argon2.wipeArray(chars);
        }
    }

    @Override
    public boolean matches(String plainPassword, String hash) {
        char[] chars = plainPassword.toCharArray();
        try {
            return argon2.verify(hash, chars);
        } finally {
            argon2.wipeArray(chars);
        }
    }
}
