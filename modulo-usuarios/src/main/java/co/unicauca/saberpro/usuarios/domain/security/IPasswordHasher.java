package co.unicauca.saberpro.usuarios.domain.security;

/**
 * Abstraccion para cifrar y verificar contrasenas (RNF-08: las contrasenas
 * deben almacenarse cifradas). El servicio de usuarios depende de esta
 * interfaz, nunca de una libreria de hashing concreta, siguiendo la misma
 * logica de inversion de dependencias aplicada a {@code IUserRepository}.
 */
public interface IPasswordHasher {

    String hash(String plainPassword);

    boolean matches(String plainPassword, String hash);
}
