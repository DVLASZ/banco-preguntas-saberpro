package co.unicauca.saberpro.revision.presentation;

import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.revision.domain.AsignacionRevisionService;
import co.unicauca.saberpro.revision.domain.Revisor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla "Asignación de Revisores" del Administrador (HU-04): muestra las
 * preguntas pendientes de revisión con su autor (criterio 1), permite elegir
 * uno o más revisores disponibles para la pregunta seleccionada (el autor no
 * se ofrece, criterio 4) y, al pulsar "Asignar", registra la asignación,
 * pasa la pregunta a "En revisión" y notifica (simulado) a cada revisor
 * (criterio 2), o muestra el error del criterio 3 si no se marcó ninguno.
 * Imita el estilo (colores, márgenes, fuentes) de {@code GUIRevisor} en
 * modulo-preguntas.
 */
public class GUIAsignacionRevisores extends JFrame {

    private static final Color GRIS_TEXTO = new Color(0x475569);

    private final AsignacionRevisionService service;
    /** Usuario del Administrador que asigna (queda registrado en cada asignación). */
    private final String administrador;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"Id", "Nombre", "Autor"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tablaPreguntas = new JTable(modeloTabla);
    private final JPanel panelCasillas = new JPanel();
    private final JLabel etiquetaSinSeleccion = new JLabel("Seleccione una pregunta");
    private final JButton btnAsignar = new JButton("Asignar");

    private final List<JCheckBox> casillasRevisores = new ArrayList<>();
    private final List<Revisor> revisoresMostrados = new ArrayList<>();

    public GUIAsignacionRevisores(AsignacionRevisionService service, String administrador) {
        super("Banco de Preguntas Saber Pro - Asignación de Revisores");
        this.service = service;
        this.administrador = administrador;

        construirInterfaz();
        cargarPreguntasPendientes();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(720, 620);
        setLocation(700, 120);
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        tablaPreguntas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionListener listener = e -> {
            if (!e.getValueIsAdjusting()) {
                recalcularRevisoresDisponibles();
            }
        };
        tablaPreguntas.getSelectionModel().addListSelectionListener(listener);
        JScrollPane scrollTabla = new JScrollPane(tablaPreguntas);
        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(tituloSeccion("Preguntas pendientes de revisión"));
        panelTabla.add(scrollTabla, BorderLayout.CENTER);

        panelCasillas.setLayout(new BoxLayout(panelCasillas, BoxLayout.Y_AXIS));
        panelCasillas.setBackground(Color.WHITE);
        JScrollPane scrollCasillas = new JScrollPane(panelCasillas);
        scrollCasillas.setBorder(BorderFactory.createEmptyBorder());
        JPanel panelRevisores = new JPanel(new BorderLayout());
        panelRevisores.setBackground(Color.WHITE);
        panelRevisores.setBorder(tituloSeccion("Revisores disponibles"));
        panelRevisores.add(scrollCasillas, BorderLayout.CENTER);
        mostrarSinSeleccion();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTabla, panelRevisores);
        split.setResizeWeight(0.55);
        split.setBorder(null);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panelAcciones.setOpaque(false);
        btnAsignar.putClientProperty("JButton.buttonType", "default");
        btnAsignar.addActionListener(e -> asignar());
        panelAcciones.add(btnAsignar);

        add(split, BorderLayout.CENTER);
        add(panelAcciones, BorderLayout.SOUTH);
    }

    private TitledBorder tituloSeccion(String titulo) {
        TitledBorder borde = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0)), titulo);
        borde.setTitleFont(borde.getTitleFont().deriveFont(Font.BOLD, 13f));
        borde.setTitleColor(GRIS_TEXTO);
        return borde;
    }

    private void cargarPreguntasPendientes() {
        modeloTabla.setRowCount(0);
        for (Question pregunta : service.preguntasPendientes()) {
            modeloTabla.addRow(new Object[]{pregunta.getId(), pregunta.getNombre(), pregunta.getAutor()});
        }
        mostrarSinSeleccion();
    }

    private void mostrarSinSeleccion() {
        panelCasillas.removeAll();
        casillasRevisores.clear();
        revisoresMostrados.clear();
        etiquetaSinSeleccion.setForeground(GRIS_TEXTO);
        panelCasillas.add(etiquetaSinSeleccion);
        panelCasillas.revalidate();
        panelCasillas.repaint();
    }

    private void recalcularRevisoresDisponibles() {
        int fila = tablaPreguntas.getSelectedRow();
        if (fila < 0) {
            mostrarSinSeleccion();
            return;
        }
        String idPregunta = (String) modeloTabla.getValueAt(fila, 0);

        panelCasillas.removeAll();
        casillasRevisores.clear();
        revisoresMostrados.clear();
        for (Revisor revisor : service.revisoresDisponibles(idPregunta)) {
            JCheckBox casilla = new JCheckBox(revisor.nombreCompleto() + " (" + revisor.usuario() + ")");
            casilla.setOpaque(false);
            casillasRevisores.add(casilla);
            revisoresMostrados.add(revisor);
            panelCasillas.add(casilla);
        }
        panelCasillas.revalidate();
        panelCasillas.repaint();
    }

    private void asignar() {
        int fila = tablaPreguntas.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta",
                    "Asignación de revisores", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String idPregunta = (String) modeloTabla.getValueAt(fila, 0);

        List<String> usuariosMarcados = new ArrayList<>();
        for (int i = 0; i < casillasRevisores.size(); i++) {
            if (casillasRevisores.get(i).isSelected()) {
                usuariosMarcados.add(revisoresMostrados.get(i).usuario());
            }
        }

        try {
            service.asignarRevisores(idPregunta, usuariosMarcados, administrador);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Asignación de revisores", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Pregunta " + idPregunta + " asignada a " + usuariosMarcados.size()
                        + " revisor(es). Se notificó por correo (simulado).",
                "Asignación de revisores", JOptionPane.INFORMATION_MESSAGE);
        cargarPreguntasPendientes();
    }
}
