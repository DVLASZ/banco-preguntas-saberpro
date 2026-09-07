package co.unicauca.saberpro.usuarios.domain.security;

/**
 * Fabrica (singleton) que instancia la implementacion concreta de
 * {@link IPasswordHasher} a usar. Cambiar de Argon2 a otro algoritmo (por
 * ejemplo BCrypt) solo implica agregar una nueva clase y un nuevo case
 * aqui, sin tocar el servicio de usuarios (Abierto/Cerrado).
 */
public class PasswordHasherFactory {

    private static PasswordHasherFactory instance;

    private PasswordHasherFactory() {
    }

    public static PasswordHasherFactory getInstance() {
        if (instance == null) {
            instance = new PasswordHasherFactory();
        }
        return instance;
    }

    public IPasswordHasher getHasher(String type) {
        IPasswordHasher result = null;

        switch (type) {
            case "default":
            case "argon2":
                result = new Argon2PasswordHasher();
                break;
        }

        return result;
    }
}
