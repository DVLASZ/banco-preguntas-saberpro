package co.unicauca.saberpro.usuarios.ui;

import co.unicauca.saberpro.usuarios.domain.Role;
import co.unicauca.saberpro.usuarios.domain.UserStatus;
import co.unicauca.saberpro.usuarios.domain.service.UserService;
import co.unicauca.saberpro.usuarios.domain.service.exception.DuplicateUsernameException;
import co.unicauca.saberpro.usuarios.domain.service.exception.InvalidPasswordException;

import javax.swing.*;
import java.awt.*;

/**
 * Formulario de registro de usuarios (RF-01, RF-03). Solo conoce
 * {@link UserService}: no sabe si detras hay SQLite, Argon2 o cualquier
 * otra tecnologia (DIP).
 */
public class RegisterFrame extends JFrame {

    private final UserService userService;

    private final JTextField usernameField = new JTextField(20);
    private final JTextField fullNameField = new JTextField(20);
    private final JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
    private final JComboBox<UserStatus> statusCombo = new JComboBox<>(UserStatus.values());
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmPasswordField = new JPasswordField(20);

    public RegisterFrame(UserService userService) {
        super("Banco de Preguntas Saber Pro - Registro de usuario");
        this.userService = userService;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(c, row++, "Usuario (login):", usernameField);
        addRow(c, row++, "Nombre completo:", fullNameField);
        addRow(c, row++, "Rol:", roleCombo);
        addRow(c, row++, "Estado:", statusCombo);
        addRow(c, row++, "Contrasena:", passwordField);
        addRow(c, row++, "Confirmar contrasena:", confirmPasswordField);

        JButton registerButton = new JButton("Registrar");
        registerButton.addActionListener(e -> onRegister());
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        add(registerButton, c);

        pack();
        setLocationRelativeTo(null);
    }

    private void addRow(GridBagConstraints c, int row, String label, JComponent field) {
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = row;
        add(new JLabel(label), c);
        c.gridx = 1;
        add(field, c);
    }

    private void onRegister() {
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        Role role = (Role) roleCombo.getSelectedItem();
        UserStatus status = (UserStatus) statusCombo.getSelectedItem();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (!password.equals(confirmPassword)) {
            showError("Las contrasenas no coinciden");
            return;
        }

        try {
            userService.register(username, fullName, role, status, password);
            JOptionPane.showMessageDialog(this, "Usuario registrado correctamente",
                    "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (InvalidPasswordException e) {
            showError("<html>" + String.join("<br>", e.getViolations()) + "</html>");
        } catch (DuplicateUsernameException | IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error de registro", JOptionPane.ERROR_MESSAGE);
    }
}
