package co.unicauca.saberpro.usuarios.ui;

import co.unicauca.saberpro.usuarios.domain.User;
import co.unicauca.saberpro.usuarios.domain.menu.MenuProviderRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Tablero generico mostrado tras iniciar sesion para los roles que aun no
 * tienen una vista real en el proyecto de curso (Administrador, Docente,
 * Estudiante). Las opciones que se listan dependen del rol del usuario
 * autenticado y se obtienen a traves de {@link MenuProviderRegistry}, sin
 * que esta clase necesite saber cuantos roles existen ni como se arma cada
 * menu (OCP). Autor de preguntas y Revisor ya tienen su propia ventana
 * ({@code GUIQuestions}, {@code GUIRevisor}) y no pasan por aqui — ver
 * {@link SesionRouter}.
 */
public class DashboardFrame extends JFrame {

    public DashboardFrame(User user, MenuProviderRegistry menuProviderRegistry) {
        super("Banco de Preguntas Saber Pro - Tablero (" + user.getRole().getDisplayName() + ")");
        buildUi(user, menuProviderRegistry);
    }

    private void buildUi(User user, MenuProviderRegistry menuProviderRegistry) {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel welcomeLabel = new JLabel(
                "Bienvenido(a), " + user.getFullName() + "  —  Rol: " + user.getRole().getDisplayName());
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(welcomeLabel, BorderLayout.NORTH);

        List<String> options = menuProviderRegistry.menuOptionsFor(user.getRole());
        DefaultListModel<String> listModel = new DefaultListModel<>();
        options.forEach(listModel::addElement);
        JList<String> menuList = new JList<>(listModel);
        menuList.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(menuList), BorderLayout.CENTER);

        JLabel nota = new JLabel(
                "<html>Estas opciones aun no tienen una vista propia en este taller; se implementarán "
                        + "más adelante en el proyecto de curso.</html>");
        nota.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        nota.setForeground(new Color(0x64748B));
        add(nota, BorderLayout.SOUTH);

        setSize(420, 360);
        setLocationRelativeTo(null);
    }
}
