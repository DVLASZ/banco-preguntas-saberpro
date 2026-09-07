package co.unicauca.saberpro.usuarios.domain.validation;

/**
 * Fabrica (singleton) que instancia la implementacion concreta de
 * {@link IPasswordPolicy} a usar. Agregar una politica mas estricta en el
 * futuro solo implica una nueva clase y un nuevo case aqui.
 */
public class PasswordPolicyFactory {

    private static PasswordPolicyFactory instance;

    private PasswordPolicyFactory() {
    }

    public static PasswordPolicyFactory getInstance() {
        if (instance == null) {
            instance = new PasswordPolicyFactory();
        }
        return instance;
    }

    public IPasswordPolicy getPolicy(String type) {
        IPasswordPolicy result = null;

        switch (type) {
            case "default":
                result = new DefaultPasswordPolicy();
                break;
        }

        return result;
    }
}
