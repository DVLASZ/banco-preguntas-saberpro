package co.unicauca.saberpro.preguntas.simulacro.presentation;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.simulacro.domain.Simulacro;
import co.unicauca.saberpro.preguntas.simulacro.domain.SimulacroService;
import co.unicauca.saberpro.preguntas.simulacro.domain.exception.SinPreguntasDisponiblesException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Ventana del rol <b>Docente</b> (HU-12, RF-21/RF-22): genera un simulacro
 * para un grupo, filtrando por competencia, tema y/o dificultad entre las
 * preguntas ya {@code PUBLICADA}s del banco. Solo conoce
 * {@link SimulacroService} — no sabe si las preguntas o los simulacros
 * viven en memoria, SQLite o cualquier otra tecnología (DIP).
 */
public class GUIDocente extends JFrame {

    private static final Color GRIS_TEXTO = new Color(0x475569);
    private static final String OPCION_TODAS = "Todas";
    private static final String OPCION_TODOS = "Todos";

    private final SimulacroService service;

    private final JComboBox<Object> comboCompetencia = new JComboBox<>();
    private final JTextField txtTema = new JTextField();
    private final JComboBox<Object> comboDificultad = new JComboBox<>();
    private final JTextField txtGrupo = new JTextField();
    private final JTextField txtCantidadPreguntas = new JTextField("10");
    private final JTextField txtTiempoMaximo = new JTextField("45");
    private final JButton btnGenerar = new JButton("Generar simulacro");

    private final DefaultListModel<Simulacro> modeloSimulacros = new DefaultListModel<>();
    private final JList<Simulacro> listaSimulacros = new JList<>(modeloSimulacros);

    public GUIDocente(SimulacroService service) {
        super("Banco de Preguntas Saber Pro - Docente");
        this.service = service;
        construirInterfaz();
        cargarSimulacros();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 620);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel rol = new JLabel("Rol: Docente");
        rol.setForeground(GRIS_TEXTO);
        rol.setFont(rol.getFont().deriveFont(Font.ITALIC, 12f));

        comboCompetencia.addItem(OPCION_TODAS);
        for (Competencia competencia : Competencia.values()) {
            comboCompetencia.addItem(competencia);
        }
        comboDificultad.addItem(OPCION_TODOS);
        for (Dificultad dificultad : Dificultad.values()) {
            comboDificultad.addItem(dificultad);
        }

        JPanel panelFiltros = new JPanel(new GridBagLayout());
        panelFiltros.setBackground(Color.WHITE);
        panelFiltros.setBorder(tituloSeccion("Filtros para el simulacro"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        agregarCampo(panelFiltros, gbc, "Grupo:", txtGrupo);
        agregarCampo(panelFiltros, gbc, "Competencia:", comboCompetencia);
        agregarCampo(panelFiltros, gbc, "Tema (opcional):", txtTema);
        agregarCampo(panelFiltros, gbc, "Dificultad:", comboDificultad);
        agregarCampo(panelFiltros, gbc, "Cantidad de preguntas:", txtCantidadPreguntas);
        agregarCampo(panelFiltros, gbc, "Tiempo máximo (minutos):", txtTiempoMaximo);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnGenerar.putClientProperty("JButton.buttonType", "default");
        btnGenerar.addActionListener(e -> generarSimulacro());
        panelFiltros.add(btnGenerar, gbc);

        JPanel panelNorte = new JPanel(new BorderLayout(4, 4));
        panelNorte.setOpaque(false);
        panelNorte.add(rol, BorderLayout.NORTH);
        panelNorte.add(panelFiltros, BorderLayout.CENTER);

        listaSimulacros.setBorder(new EmptyBorder(6, 6, 6, 6));
        JPanel panelLista = new JPanel(new BorderLayout());
        panelLista.setBackground(Color.WHITE);
        panelLista.setBorder(tituloSeccion("Simulacros generados"));
        panelLista.add(new JScrollPane(listaSimulacros), BorderLayout.CENTER);

        add(panelNorte, BorderLayout.NORTH);
        add(panelLista, BorderLayout.CENTER);
    }

    private TitledBorder tituloSeccion(String titulo) {
        TitledBorder borde = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0)), titulo);
        borde.setTitleFont(borde.getTitleFont().deriveFont(Font.BOLD, 13f));
        borde.setTitleColor(GRIS_TEXTO);
        return borde;
    }

    private JLabel etiqueta(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(GRIS_TEXTO);
        return label;
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, String etiqueta, JComponent campo) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        panel.add(etiqueta(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(campo, gbc);
        gbc.gridy++;
    }

    private void cargarSimulacros() {
        modeloSimulacros.clear();
        for (Simulacro simulacro : service.listarSimulacros()) {
            modeloSimulacros.addElement(simulacro);
        }
    }

    private void generarSimulacro() {
        String grupo = txtGrupo.getText().trim();
        if (grupo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El grupo es obligatorio", "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Competencia competencia = comboCompetencia.getSelectedItem() instanceof Competencia
                ? (Competencia) comboCompetencia.getSelectedItem() : null;
        String tema = txtTema.getText().trim();
        Dificultad dificultad = comboDificultad.getSelectedItem() instanceof Dificultad
                ? (Dificultad) comboDificultad.getSelectedItem() : null;

        int cantidad;
        int tiempoMaximo;
        try {
            cantidad = Integer.parseInt(txtCantidadPreguntas.getText().trim());
            tiempoMaximo = Integer.parseInt(txtTiempoMaximo.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La cantidad de preguntas y el tiempo máximo deben ser números",
                    "Datos inválidos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Simulacro creado = service.generarSimulacro(grupo, competencia, tema, dificultad, cantidad, tiempoMaximo);
            cargarSimulacros();
            listaSimulacros.setSelectedValue(creado, true);
            JOptionPane.showMessageDialog(this,
                    "Simulacro " + creado.getId() + " generado con " + creado.getPreguntas().size() + " preguntas.",
                    "Simulacro generado", JOptionPane.INFORMATION_MESSAGE);
        } catch (SinPreguntasDisponiblesException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo generar el simulacro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
