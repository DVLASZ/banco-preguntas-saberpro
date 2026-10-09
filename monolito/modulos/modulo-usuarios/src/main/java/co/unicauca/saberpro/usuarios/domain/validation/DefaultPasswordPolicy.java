package co.unicauca.saberpro.usuarios.domain.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Politica de contrasenas exigida por la guia del taller: minimo 6
 * caracteres, al menos un digito, al menos un caracter especial y al menos
 * una mayuscula.
 */
public class DefaultPasswordPolicy implements IPasswordPolicy {

    private static final int MIN_LENGTH = 6;
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[^A-Za-z0-9]");

    @Override
    public List<String> validate(String plainPassword) {
        List<String> violations = new ArrayList<>();
        if (plainPassword == null || plainPassword.length() < MIN_LENGTH) {
            violations.add("La contrasena debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (plainPassword == null || !DIGIT.matcher(plainPassword).find()) {
            violations.add("La contrasena debe contener al menos un digito");
        }
        if (plainPassword == null || !UPPERCASE.matcher(plainPassword).find()) {
            violations.add("La contrasena debe contener al menos una mayuscula");
        }
        if (plainPassword == null || !SPECIAL_CHAR.matcher(plainPassword).find()) {
            violations.add("La contrasena debe contener al menos un caracter especial");
        }
        return violations;
    }
}
