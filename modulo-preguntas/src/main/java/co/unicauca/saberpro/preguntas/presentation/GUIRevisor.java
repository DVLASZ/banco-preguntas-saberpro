package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.FuenteDePreguntasParaRevisar;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Vista (MVC) del rol <b>Revisor</b>: muestra una pregunta ya enviada a
 * revisión y solo permite Aprobarla o Rechazarla (RF-16) — a diferencia del
 * Autor ({@link GUIQuestions}), no edita el contenido de la pregunta ni
 * tiene un selector de estado libre. Las decisiones las toma el
 * {@link RevisionController}, que las registra en el modelo
 * ({@link QuestionService#cambiarEstado}) y así se notifica a las vistas
 * observadoras ({@link GUIObserver1}, {@link GUIObserver2}).
 */
public class GUIRevisor extends JFrame implements RevisionVista {

    private static final Color GRIS_TEXTO = new Color(0x475569);
    private static final Color FONDO_CAMPO = new Color(0xF1F5F9);

    private final RevisionController controlador;
    private final String usuario;

    private final JComboBox<Question> comboPreguntas = new JComboBox<>();
    private final JButton btnCargar = new JButton("Cargar pregunta");

    private final JTextField txtId = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JTextArea txtContexto = new JTextArea(3, 30);
    private final JTextArea txtEnunciado = new JTextArea(2, 30);
    private final JTextArea txtJustificacion = new JTextArea(3, 30);
    private final JTextField txtOpcionA = new JTextField();
    private final JTextField txtOpcionB = new JTextField();
    private final JTextField txtOpcionC = new JTextField();
    private final JTextField txtOpcionD = new JTextField();
    private final JTextField txtRespuestaCorrecta = new JTextField();
    private final EstadoBadge badgeEstadoActual = new EstadoBadge();
    private final JButton btnAprobar = new JButton("Aprobar");
    private final JButton btnRechazar = new JButton("Rechazar");

    public GUIRevisor(QuestionService service, FuenteDePreguntasParaRevisar fuente, String usuario) {
        super("Banco de Preguntas Saber Pro - Revisor");
        this.usuario = usuario;
        this.controlador = new RevisionController(service, fuente, usuario, this);
        construirInterfaz();
        habilitarAcciones(false);
        controlador.iniciar();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(640, 780);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel rol = new JLabel("Rol: Revisor — usuario: " + usuario);
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
        agregarAreaSoloLectura(panelFormulario, gbc, "Contexto:", txtContexto);
        agregarAreaSoloLectura(panelFormulario, gbc, "Pregunta:", txtEnunciado);

        agregarCampo(panelFormulario, gbc, "A.", txtOpcionA);
        agregarCampo(panelFormulario, gbc, "B.", txtOpcionB);
        agregarCampo(panelFormulario, gbc, "C.", txtOpcionC);
        agregarCampo(panelFormulario, gbc, "D.", txtOpcionD);
        agregarCampo(panelFormulario, gbc, "Respuesta correcta:", txtRespuestaCorrecta);
        agregarAreaSoloLectura(panelFormulario, gbc, "Justificación:", txtJustificacion);

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
        btnRechazar.addActionListener(e -> controlador.rechazar());
        btnAprobar.addActionListener(e -> controlador.aprobar());
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

    private void agregarAreaSoloLectura(JPanel panel, GridBagConstraints gbc, String etiqueta, JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setBackground(FONDO_CAMPO);
        area.setFont(area.getFont().deriveFont(13f));
        area.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0)));
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(etiqueta(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(scroll, gbc);
        gbc.gridy++;
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

    @Override
    public void mostrarPreguntasPorRevisar(List<Question> preguntas) {
        comboPreguntas.removeAllItems();
        for (Question pregunta : preguntas) {
            comboPreguntas.addItem(pregunta);
        }
    }

    private void cargarPreguntaSeleccionada() {
        Question seleccionada = (Question) comboPreguntas.getSelectedItem();
        if (seleccionada != null) {
            controlador.cargarPregunta(seleccionada.getId());
        }
    }

    @Override
    public void mostrarPregunta(Question pregunta, boolean puedeDecidir) {
        txtId.setText(pregunta.getId());
        txtNombre.setText(pregunta.getNombre());
        txtContexto.setText(pregunta.getContexto());
        txtEnunciado.setText(pregunta.getEnunciado());
        txtOpcionA.setText(pregunta.getOpciones().getOpcionA());
        txtOpcionB.setText(pregunta.getOpciones().getOpcionB());
        txtOpcionC.setText(pregunta.getOpciones().getOpcionC());
        txtOpcionD.setText(pregunta.getOpciones().getOpcionD());
        txtRespuestaCorrecta.setText(String.valueOf(pregunta.getRespuestaCorrecta()));
        txtJustificacion.setText(pregunta.getJustificacion());
        badgeEstadoActual.mostrar(pregunta.getEstado());
        habilitarAcciones(puedeDecidir);
    }

    @Override
    public void mostrarDecision(String idPregunta, EstadoPregunta decision) {
        badgeEstadoActual.mostrar(decision);
        habilitarAcciones(false);
        JOptionPane.showMessageDialog(this, "Pregunta " + idPregunta + " marcada como: " + decision,
                "Revisión registrada", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void mostrarError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    private void habilitarAcciones(boolean habilitado) {
        btnAprobar.setEnabled(habilitado);
        btnRechazar.setEnabled(habilitado);
    }
}
