package co.unicauca.saberpro.revision.presentation;

import co.unicauca.saberpro.revision.domain.AsignacionRevisionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Pantalla "Asignación de Revisores" del Administrador (HU-04).
 *
 * <p>TODO(HU-04): reemplazar este marcador por la pantalla real: una tabla con las
 * preguntas pendientes de revisión (id, nombre y autor); al elegir una, la lista
 * de revisores disponibles con casillas (el autor no se ofrece: no aparece en la lista); y un botón
 * "Asignar" que llama a {@code AsignacionRevisionService.asignarRevisores}, muestra
 * el error "Debe seleccionar al menos un revisor" si no se marcó ninguno y avisa que
 * se notificó por correo. Para el estilo (colores, márgenes, fuentes) mira
 * {@code GUIRevisor} y {@code GUIQuestions} en modulo-preguntas.
 */
public class GUIAsignacionRevisores extends JFrame {

    private final AsignacionRevisionService service;
    /** Usuario del Administrador que asigna (queda registrado en cada asignación). */
    private final String administrador;

    public GUIAsignacionRevisores(AsignacionRevisionService service, String administrador) {
        super("Banco de Preguntas Saber Pro - Asignación de Revisores");
        this.service = service;
        this.administrador = administrador;

        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel marcador = new JLabel("<html><b>Asignación de revisores (HU-04)</b><br><br>"
                + "Pantalla pendiente de implementar.</html>", SwingConstants.CENTER);
        add(marcador, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(720, 520);
        setLocation(700, 120);
    }
}
