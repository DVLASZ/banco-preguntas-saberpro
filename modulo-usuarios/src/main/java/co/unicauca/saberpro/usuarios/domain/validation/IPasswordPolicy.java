package co.unicauca.saberpro.usuarios.domain.validation;

import java.util.List;

/**
 * Abstraccion para validar reglas de complejidad de contrasenas. Se aisla
 * en su propia interfaz (ademas de {@code IPasswordHasher}) para que las
 * reglas de negocio de complejidad puedan cambiar sin afectar el algoritmo
 * de cifrado ni el servicio de usuarios.
 */
public interface IPasswordPolicy {

    /**
     * @return lista de violaciones encontradas; vacia si la contrasena es valida.
     */
    List<String> validate(String plainPassword);

    default boolean isValid(String plainPassword) {
        return validate(plainPassword).isEmpty();
    }
}
