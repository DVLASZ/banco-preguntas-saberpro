package co.unicauca.saberpro.usuarios.domain.access;

/**
 * Fabrica (singleton) que se encarga de instanciar {@link SqliteUserRepository}
 * o cualquier otra implementacion de {@link IUserRepository} que se cree en
 * el futuro (por ejemplo, para Postgres o MySQL), sin que el resto de la
 * aplicacion tenga que saber cual se esta usando.
 */
public class UserRepositoryFactory {

    private static UserRepositoryFactory instance;

    private UserRepositoryFactory() {
    }

    /** Clase singleton. */
    public static UserRepositoryFactory getInstance() {
        if (instance == null) {
            instance = new UserRepositoryFactory();
        }
        return instance;
    }

    /**
     * Crea una instancia concreta de la jerarquia IUserRepository.
     *
     * @param type cadena que indica que tipo de clase hija debe instanciar
     * @return una clase hija de la abstraccion IUserRepository
     */
    public IUserRepository getRepository(String type) {
        IUserRepository result = null;

        switch (type) {
            case "default":
            case "sqlite":
                result = new SqliteUserRepository();
                break;
        }

        return result;
    }
}
