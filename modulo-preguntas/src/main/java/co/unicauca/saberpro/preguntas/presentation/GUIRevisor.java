package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Ventana del rol <b>Revisor</b>: evalúa una pregunta ya enviada a
 * revisión y solo puede Aprobarla o Rechazarla (RF-16) — a diferencia del
 * Autor ({@link GUIQuestions}), no edita el contenido de la pregunta ni
 * tiene un selector de estado libre. Cada acción dispara
 * {@link QuestionService#cambiarEstado} y, por lo tanto, notifica a las
 * vistas observadoras ({@link GUIObserver1}, {@link GUIObserver2}).
 */
public class GUIRevisor extends JFrame {

    private static final Color GRIS_TEXTO = new Color(0x475569);
    private static final Color FONDO_CAMPO = new Color(0xF1F5F9);

    private final QuestionService service;

    private final JComboBox<Question> comboPreguntas = new JComboBox<>();
    private final JButton btnCargar = new JButton("Cargar pregunta");

    private final JTextField txtId = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JTextArea txtEnunciado = new JTextArea(3, 30);
    private final JTextField txtOpcionA = new JTextField();
    private final JTextField txtOpcionB = new JTextField();
    private final JTextField txtOpcionC = new JTextField();
    private final JTextField txtOpcionD = new JTextField();
    private final JTextField txtRespuestaCorrecta = new JTextField();
    private final EstadoBadge badgeEstadoActual = new EstadoBadge();
    private final JButton btnAprobar = new JButton("Aprobar");
    private final JButton btnRechazar = new JButton("Rechazar");

    private Question preguntaCargada;

    public GUIRevisor(QuestionService service) {
        super("Banco de Preguntas Saber Pro - Revisor");
        this.service = service;
        construirInterfaz();
        cargarComboPreguntas();
        habilitarAcciones(false);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 600);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel rol = new JLabel("Rol: Revisor");
        rol.setForeground(GRIS_TEXTO);
        rol.setFont(rol.getFont().deriveFont(Font.ITALIC, 12f));

        JPanel panelSeleccion = new JPanel(new BorderLayout(8, 8));
        panelSeleccion.setBackground(Color.WHITE);
        panelSeleccion.setBorder(tituloSeccion("Seleccionar pregunta"));
        panelSeleccion.add(comboPreguntas, BorderLayout.CENTER);
        panelSeleccion.add(btnCargar, BorderLayout.EAST);
        btnCargar.putClientProperty("JButton.buttonType", "default");
        btnCargar.addActionListener(e -> cargarPreguntaSeleccionada());

        JPanel panelNorte = new JPanel(new BorderLayout(4, 4));
        panelNorte.setOpaque(false);
        panelNorte.add(rol, BorderLayout.NORTH);
        panelNorte.add(panelSeleccion, BorderLayout.CENTER);

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBackground(Color.WHITE);
        panelFormulario.setBorder(tituloSeccion("Información de la pregunta (solo lectura)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        agregarCampo(panelFormulario, gbc, "Id:", txtId);
        agregarCampo(panelFormulario, gbc, "Nombre:", txtNombre);

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panelFormulario.add(etiqueta("Pregunta:"), gbc);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 1;
        gbc.weightx = 1;
        txtEnunciado.setLineWrap(true);
        txtEnunciado.setWrapStyleWord(true);
        txtEnunciado.setEditable(false);
        txtEnunciado.setBackground(FONDO_CAMPO);
        txtEnunciado.setFont(txtEnunciado.getFont().deriveFont(13f));
        txtEnunciado.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane scrollEnunciado = new JScrollPane(txtEnunciado);
        scrollEnunciado.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0)));
        panelFormulario.add(scrollEnunciado, gbc);
        gbc.gridy++;

        agregarCampo(panelFormulario, gbc, "A.", txtOpcionA);
        agregarCampo(panelFormulario, gbc, "B.", txtOpcionB);
        agregarCampo(panelFormulario, gbc, "C.", txtOpcionC);
        agregarCampo(panelFormulario, gbc, "D.", txtOpcionD);
        agregarCampo(panelFormulario, gbc, "Respuesta correcta:", txtRespuestaCorrecta);

        gbc.gridx = 0;
        gbc.weightx = 0;
        panelFormulario.add(etiqueta("Estado actual:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel envolturaBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        envolturaBadge.setOpaque(false);
        envolturaBadge.add(badgeEstadoActual);
        panelFormulario.add(envolturaBadge, gbc);

        for (JTextField campo : new JTextField[]{txtId, txtNombre, txtOpcionA, txtOpcionB,
                txtOpcionC, txtOpcionD, txtRespuestaCorrecta}) {
            campo.setEditable(false);
            campo.setBackground(FONDO_CAMPO);
        }

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panelAcciones.setOpaque(false);
        btnRechazar.addActionListener(e -> decidir(EstadoPregunta.RECHAZADA));
        btnAprobar.addActionListener(e -> decidir(EstadoPregunta.APROBADA));
        btnAprobar.putClientProperty("JButton.buttonType", "default");
        panelAcciones.add(btnRechazar);
        panelAcciones.add(btnAprobar);

        add(panelNorte, BorderLayout.NORTH);
        add(panelFormulario, BorderLayout.CENTER);
        add(panelAcciones, BorderLayout.SOUTH);
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
        panel.add(etiqueta(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(campo, gbc);
        gbc.gridy++;
    }

    private void cargarComboPreguntas() {
        comboPreguntas.removeAllItems();
        for (Question pregunta : service.listarPreguntas()) {
            comboPreguntas.addItem(pregunta);
        }
    }

    private void cargarPreguntaSeleccionada() {
        Question seleccionada = (Question) comboPreguntas.getSelectedItem();
        if (seleccionada == null) {
            return;
        }
        preguntaCargada = service.obtenerPregunta(seleccionada.getId());

        // Al tomar una pregunta pendiente para revisarla, pasa
        // automáticamente a "En revisión" (RF-15): el solo hecho de que
        // el Revisor la abra ya inicia su evaluación.
        if (preguntaCargada.getEstado() == EstadoPregunta.PENDIENTE_REVISION) {
            service.cambiarEstado(preguntaCargada.getId(), EstadoPregunta.EN_REVISION);
            preguntaCargada = service.obtenerPregunta(preguntaCargada.getId());
        }

        txtId.setText(preguntaCargada.getId());
        txtNombre.setText(preguntaCargada.getNombre());
        txtEnunciado.setText(preguntaCargada.getEnunciado());
        txtOpcionA.setText(preguntaCargada.getOpciones().getOpcionA());
        txtOpcionB.setText(preguntaCargada.getOpciones().getOpcionB());
        txtOpcionC.setText(preguntaCargada.getOpciones().getOpcionC());
        txtOpcionD.setText(preguntaCargada.getOpciones().getOpcionD());
        txtRespuestaCorrecta.setText(String.valueOf(preguntaCargada.getRespuestaCorrecta()));
        badgeEstadoActual.mostrar(preguntaCargada.getEstado());
        habilitarAcciones(true);
    }

    private void decidir(EstadoPregunta decision) {
        if (preguntaCargada == null) {
            return;
        }
        service.cambiarEstado(preguntaCargada.getId(), decision);
        badgeEstadoActual.mostrar(decision);
        JOptionPane.showMessageDialog(this,
                "Pregunta " + preguntaCargada.getId() + " marcada como: " + decision,
                "Revisión registrada", JOptionPane.INFORMATION_MESSAGE);
    }

    private void habilitarAcciones(boolean habilitado) {
        btnAprobar.setEnabled(habilitado);
        btnRechazar.setEnabled(habilitado);
    }
}
