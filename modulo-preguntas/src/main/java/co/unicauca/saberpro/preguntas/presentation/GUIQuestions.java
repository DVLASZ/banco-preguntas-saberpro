package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import co.unicauca.saberpro.preguntas.domain.QuestionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Ventana del rol <b>Autor de preguntas</b>: redacta el contenido de sus
 * preguntas — crea preguntas nuevas y edita las existentes (nombre,
 * enunciado, opciones, respuesta correcta) — (HU-04). Al crear una
 * pregunta esta queda en {@code PENDIENTE_REVISION}: el Autor la redacta
 * y la envía, pero no decide su aprobación ni cambia su estado por
 * ningún otro medio — esa es competencia exclusiva del rol Revisor (ver
 * {@link GUIRevisor}), tal como se distribuyó en el modelo C4 del
 * Taller 3.
 */
public class GUIQuestions extends JFrame {

    private static final Color GRIS_TEXTO = new Color(0x475569);
    private static final Color FONDO_CAMPO = new Color(0xF1F5F9);

    private final QuestionService service;

    private final JComboBox<Question> comboPreguntas = new JComboBox<>();
    private final JButton btnCargar = new JButton("Cargar pregunta");
    private final JButton btnNueva = new JButton("Nueva pregunta");
    private final JButton btnGuardar = new JButton("Guardar");

    private final JTextField txtId = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JTextArea txtEnunciado = new JTextArea(3, 30);
    private final JTextField txtOpcionA = new JTextField();
    private final JTextField txtOpcionB = new JTextField();
    private final JTextField txtOpcionC = new JTextField();
    private final JTextField txtOpcionD = new JTextField();
    private final JTextField txtRespuestaCorrecta = new JTextField();
    private final JComboBox<Competencia> comboCompetencia = new JComboBox<>(Competencia.values());
    private final JTextField txtTema = new JTextField();
    private final JComboBox<Dificultad> comboDificultad = new JComboBox<>(Dificultad.values());
    private final EstadoBadge badgeEstadoActual = new EstadoBadge();

    /** {@code null} mientras se redacta una pregunta nueva aún no guardada. */
    private String idPreguntaEnEdicion;

    public GUIQuestions(QuestionService service) {
        super("Banco de Preguntas Saber Pro - Autor de Preguntas");
        this.service = service;
        construirInterfaz();
        cargarComboPreguntas();
        habilitarFormulario(false);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 700);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel rol = new JLabel("Rol: Autor de preguntas");
        rol.setForeground(GRIS_TEXTO);
        rol.setFont(rol.getFont().deriveFont(Font.ITALIC, 12f));

        JPanel panelSeleccion = new JPanel(new BorderLayout(8, 8));
        panelSeleccion.setBackground(Color.WHITE);
        panelSeleccion.setBorder(tituloSeccion("Seleccionar o crear pregunta"));
        panelSeleccion.add(comboPreguntas, BorderLayout.CENTER);
        JPanel botonesSeleccion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botonesSeleccion.setOpaque(false);
        botonesSeleccion.add(btnCargar);
        botonesSeleccion.add(btnNueva);
        panelSeleccion.add(botonesSeleccion, BorderLayout.EAST);
        btnCargar.addActionListener(e -> cargarPreguntaSeleccionada());
        btnNueva.addActionListener(e -> iniciarPreguntaNueva());

        JPanel panelNorte = new JPanel(new BorderLayout(4, 4));
        panelNorte.setOpaque(false);
        panelNorte.add(rol, BorderLayout.NORTH);
        panelNorte.add(panelSeleccion, BorderLayout.CENTER);

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBackground(Color.WHITE);
        panelFormulario.setBorder(tituloSeccion("Redacción de la pregunta"));
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
        agregarCampo(panelFormulario, gbc, "Respuesta correcta (A-D):", txtRespuestaCorrecta);
        agregarCampo(panelFormulario, gbc, "Competencia:", comboCompetencia);
        agregarCampo(panelFormulario, gbc, "Tema:", txtTema);
        agregarCampo(panelFormulario, gbc, "Dificultad:", comboDificultad);

        gbc.gridx = 0;
        gbc.weightx = 0;
        panelFormulario.add(etiqueta("Estado actual:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel envolturaBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        envolturaBadge.setOpaque(false);
        envolturaBadge.add(badgeEstadoActual);
        panelFormulario.add(envolturaBadge, gbc);

        txtId.setEditable(false);
        txtId.setBackground(FONDO_CAMPO);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panelAcciones.setOpaque(false);
        btnGuardar.putClientProperty("JButton.buttonType", "default");
        btnGuardar.addActionListener(e -> guardar());
        panelAcciones.add(btnGuardar);

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
        Question pregunta = service.obtenerPregunta(seleccionada.getId());
        idPreguntaEnEdicion = pregunta.getId();
        txtId.setText(pregunta.getId());
        txtNombre.setText(pregunta.getNombre());
        txtEnunciado.setText(pregunta.getEnunciado());
        txtOpcionA.setText(pregunta.getOpciones().getOpcionA());
        txtOpcionB.setText(pregunta.getOpciones().getOpcionB());
        txtOpcionC.setText(pregunta.getOpciones().getOpcionC());
        txtOpcionD.setText(pregunta.getOpciones().getOpcionD());
        txtRespuestaCorrecta.setText(String.valueOf(pregunta.getRespuestaCorrecta()));
        comboCompetencia.setSelectedItem(pregunta.getCompetencia());
        txtTema.setText(pregunta.getTema());
        comboDificultad.setSelectedItem(pregunta.getDificultad());
        badgeEstadoActual.mostrar(pregunta.getEstado());
        habilitarFormulario(true);
    }

    /** Limpia el formulario para redactar una pregunta completamente nueva. */
    private void iniciarPreguntaNueva() {
        idPreguntaEnEdicion = null;
        comboPreguntas.setSelectedItem(null);
        txtId.setText("(nueva)");
        txtNombre.setText("");
        txtEnunciado.setText("");
        txtOpcionA.setText("");
        txtOpcionB.setText("");
        txtOpcionC.setText("");
        txtOpcionD.setText("");
        txtRespuestaCorrecta.setText("");
        comboCompetencia.setSelectedIndex(0);
        txtTema.setText("");
        comboDificultad.setSelectedIndex(0);
        badgeEstadoActual.setText("");
        habilitarFormulario(true);
        txtNombre.requestFocusInWindow();
    }

    private void guardar() {
        String respuesta = txtRespuestaCorrecta.getText().trim();
        try {
            QuestionDistractors opciones = new QuestionDistractors(
                    txtOpcionA.getText().trim(), txtOpcionB.getText().trim(),
                    txtOpcionC.getText().trim(), txtOpcionD.getText().trim());
            char respuestaCorrecta = respuesta.isEmpty() ? ' ' : respuesta.charAt(0);
            Competencia competencia = (Competencia) comboCompetencia.getSelectedItem();
            String tema = txtTema.getText().trim();
            Dificultad dificultad = (Dificultad) comboDificultad.getSelectedItem();

            if (idPreguntaEnEdicion == null) {
                Question creada = service.crearPregunta(txtNombre.getText().trim(),
                        txtEnunciado.getText().trim(), opciones, respuestaCorrecta,
                        competencia, tema, dificultad);
                idPreguntaEnEdicion = creada.getId();
                JOptionPane.showMessageDialog(this,
                        "Pregunta " + creada.getId() + " creada y enviada a revisión.",
                        "Pregunta creada", JOptionPane.INFORMATION_MESSAGE);
            } else {
                service.actualizarContenido(idPreguntaEnEdicion, txtNombre.getText().trim(),
                        txtEnunciado.getText().trim(), opciones, respuestaCorrecta,
                        competencia, tema, dificultad);
                JOptionPane.showMessageDialog(this,
                        "Cambios guardados en " + idPreguntaEnEdicion + ".",
                        "Pregunta actualizada", JOptionPane.INFORMATION_MESSAGE);
            }

            cargarComboPreguntas();
            comboPreguntas.setSelectedItem(service.obtenerPregunta(idPreguntaEnEdicion));
            cargarPreguntaSeleccionada();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "No se pudo guardar la pregunta", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void habilitarFormulario(boolean habilitado) {
        txtNombre.setEditable(habilitado);
        txtEnunciado.setEditable(habilitado);
        txtOpcionA.setEditable(habilitado);
        txtOpcionB.setEditable(habilitado);
        txtOpcionC.setEditable(habilitado);
        txtOpcionD.setEditable(habilitado);
        txtRespuestaCorrecta.setEditable(habilitado);
        txtTema.setEditable(habilitado);
        comboCompetencia.setEnabled(habilitado);
        comboDificultad.setEnabled(habilitado);
        btnGuardar.setEnabled(habilitado);

        Color fondo = habilitado ? Color.WHITE : FONDO_CAMPO;
        for (JTextField campo : new JTextField[]{txtNombre, txtOpcionA, txtOpcionB,
                txtOpcionC, txtOpcionD, txtRespuestaCorrecta, txtTema}) {
            campo.setBackground(fondo);
        }
        txtEnunciado.setBackground(fondo);
    }
}
