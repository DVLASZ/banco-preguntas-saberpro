package co.unicauca.saberpro.usuarios.domain.access;

import co.unicauca.saberpro.usuarios.domain.Role;
import co.unicauca.saberpro.usuarios.domain.User;
import co.unicauca.saberpro.usuarios.domain.UserStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Es una implementacion que tiene libertad de hacer la implementacion del
 * contrato {@link IUserRepository}. Lo puede hacer con SQLite, Postgres,
 * MySQL u otra tecnologia; el resto de la aplicacion nunca depende de esta
 * clase directamente, solo de la interfaz.
 */
public class SqliteUserRepository implements IUserRepository {

    private static final String DEFAULT_URL = "jdbc:sqlite:saberpro.db";

    private Connection conn;

    /** Usa el archivo fisico por defecto (saberpro.db) para persistir entre ejecuciones. */
    public SqliteUserRepository() {
        this(DEFAULT_URL);
    }

    /** Permite indicar otra cadena de conexion, por ejemplo "jdbc:sqlite::memory:" en pruebas. */
    public SqliteUserRepository(String jdbcUrl) {
        connect(jdbcUrl);
        initDatabase();
    }

    @Override
    public boolean save(User newUser) {
        try {
            if (newUser == null || newUser.getUsername().isBlank()) {
                return false;
            }

            String sql = "INSERT INTO Users (Username, FullName, Role, Status, PasswordHash) "
                    + "VALUES (?, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, newUser.getUsername());
            pstmt.setString(2, newUser.getFullName());
            pstmt.setString(3, newUser.getRole().name());
            pstmt.setString(4, newUser.getStatus().name());
            pstmt.setString(5, newUser.getPasswordHash());
            pstmt.executeUpdate();

            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                newUser.setId(generatedKeys.getLong(1));
            }
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(SqliteUserRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            String sql = "SELECT Id, Username, FullName, Role, Status, PasswordHash "
                    + "FROM Users WHERE Username = ?";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            Logger.getLogger(SqliteUserRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public List<User> list() {
        List<User> users = new ArrayList<>();
        try {
            String sql = "SELECT Id, Username, FullName, Role, Status, PasswordHash FROM Users ORDER BY Username";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            Logger.getLogger(SqliteUserRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return users;
    }

    @Override
    public boolean update(User user) {
        try {
            if (user == null || user.getId() == null) {
                return false;
            }

            String sql = "UPDATE Users SET FullName = ?, Role = ?, Status = ?, PasswordHash = ? WHERE Id = ?";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getRole().name());
            pstmt.setString(3, user.getStatus().name());
            pstmt.setString(4, user.getPasswordHash());
            pstmt.setLong(5, user.getId());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(SqliteUserRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("Id"),
                rs.getString("Username"),
                rs.getString("FullName"),
                Role.valueOf(rs.getString("Role")),
                UserStatus.valueOf(rs.getString("Status")),
                rs.getString("PasswordHash")
        );
    }

    private void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS Users (\n"
                + " Id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " Username TEXT NOT NULL UNIQUE,\n"
                + " FullName TEXT NOT NULL,\n"
                + " Role TEXT NOT NULL,\n"
                + " Status TEXT NOT NULL,\n"
                + " PasswordHash TEXT NOT NULL\n"
                + ");";

        try {
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException ex) {
            Logger.getLogger(SqliteUserRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void connect(String url) {
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException ex) {
            Logger.getLogger(SqliteUserRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void disconnect() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
