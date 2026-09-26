package co.unicauca.saberpro.usuarios.ui;

import co.unicauca.saberpro.usuarios.domain.User;
import co.unicauca.saberpro.usuarios.domain.menu.MenuProviderRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * Tablero generico mostrado tras iniciar sesion para los roles que no tienen
 * su propia ventana principal (Administrador y cualquier rol sin una vista
 * dedicada). Las opciones que se listan dependen del rol del usuario
 * autenticado y se obtienen a traves de {@link MenuProviderRegistry}, sin
 * que esta clase necesite saber cuantos roles existen ni como se arma cada
 * menu (OCP). Autor de preguntas y Revisor ya tienen su propia ventana
 * ({@code GUIQuestions}, {@code GUIRevisor}) y no pasan por aqui — ver
 * {@link SesionRouter}.
 *
 * <p>Cada opcion puede tener asociada una accion (por ejemplo, abrir una
 * ventana de reporte) que el punto de composicion de la aplicacion decide y
 * pasa en {@code acciones}: esta clase no conoce que ventanas existen, solo
 * dispara la accion cuando el usuario hace doble clic en la opcion. Si una
 * opcion no tiene accion asociada, se avisa que aun no tiene vista propia.
 */
public class DashboardFrame extends JFrame {

    public DashboardFrame(User user, MenuProviderRegistry menuProviderRegistry, Map<String, Runnable> acciones) {
        super("Banco de Preguntas Saber Pro - Tablero (" + user.getRole().getDisplayName() + ")");
        buildUi(user, menuProviderRegistry, acciones);
    }

    private void buildUi(User user, MenuProviderRegistry menuProviderRegistry, Map<String, Runnable> acciones) {
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
        menuList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                String opcion = menuList.getSelectedValue();
                if (opcion == null) {
                    return;
                }
                Runnable accion = acciones.get(opcion);
                if (accion != null) {
                    accion.run();
                } else {
                    JOptionPane.showMessageDialog(DashboardFrame.this,
                            "Esta opcion aun no tiene una vista propia en este taller.",
                            "No implementado", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        add(new JScrollPane(menuList), BorderLayout.CENTER);

        JLabel nota = new JLabel(
                "<html>Haga doble clic en una opcion para abrirla. Las que aun no tienen vista propia "
                        + "en este taller lo avisan al seleccionarlas.</html>");
        nota.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        nota.setForeground(new Color(0x64748B));
        add(nota, BorderLayout.SOUTH);

        setSize(420, 360);
        setLocationRelativeTo(null);
    }
}
