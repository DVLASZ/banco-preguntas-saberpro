package co.unicauca.saberpro.microkernel.presentation;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.core.QuestionMicrokernel;
import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.Question;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Ventana adicional del rol <b>Autor de preguntas</b> (Taller 5): genera
 * preguntas usando el Microkernel de plugins en vez del formulario manual
 * de {@code GUIQuestions}. Solo conoce {@link QuestionMicrokernel} — no
 * sabe qué plugins existen ni cómo se cargan (Reflexión, patrón
 * Microkernel).
 */
public class GUIMicrokernel extends JFrame {

    private static final Color GRIS_TEXTO = new Color(0x475569);
    private static final String[][] TIPOS = {
            {"Selección múltiple", "MULTIPLE_CHOICE"},
            {"Caso de estudio", "CASO"},
            {"Multimedia", "MULTIMEDIA"},
    };
    private static final String[] LETRAS = {"A", "B", "C", "D"};

    private final QuestionMicrokernel microkernel;

    private final JComboBox<String> comboTipo = new JComboBox<>();
    private final JTextField txtTitulo = new JTextField();
    private final JTextArea txtEnunciado = new JTextArea(3, 30);
    private final JTextField[] txtOpciones = new JTextField[4];
    private final JComboBox<String> comboRespuestaCorrecta = new JComboBox<>(LETRAS);
    private final JComboBox<Competencia> comboCompetencia = new JComboBox<>(Competencia.values());
    private final JTextField txtTema = new JTextField();
    private final JComboBox<Dificultad> comboDificultad = new JComboBox<>(Dificultad.values());
    private final JTextField txtRecursoMultimedia = new JTextField();
    private final JButton btnGenerar = new JButton("Generar pregunta");

    private final DefaultListModel<Question> modeloGeneradas = new DefaultListModel<>();
    private final JList<Question> listaGeneradas = new JList<>(modeloGeneradas);

    public GUIMicrokernel(QuestionMicrokernel microkernel) {
        super("Banco de Preguntas Saber Pro - Generador de preguntas (Microkernel)");
        this.microkernel = microkernel;
        construirInterfaz();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 720);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel rol = new JLabel("Rol: Autor de preguntas — vía plugin (Taller 5)");
        rol.setForeground(GRIS_TEXTO);
        rol.setFont(rol.getFont().deriveFont(Font.ITALIC, 12f));

        for (String[] tipo : TIPOS) {
            comboTipo.addItem(tipo[0]);
        }
        comboTipo.addActionListener(e -> actualizarDisponibilidadRecurso());

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBackground(Color.WHITE);
        panelFormulario.setBorder(tituloSeccion("Nueva pregunta"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        agregarCampo(panelFormulario, gbc, "Tipo de pregunta:", comboTipo);
        agregarCampo(panelFormulario, gbc, "Título:", txtTitulo);

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panelFormulario.add(etiqueta("Enunciado:"), gbc);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 1;
        gbc.weightx = 1;
        txtEnunciado.setLineWrap(true);
        txtEnunciado.setWrapStyleWord(true);
        JScrollPane scrollEnunciado = new JScrollPane(txtEnunciado);
        scrollEnunciado.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0)));
        panelFormulario.add(scrollEnunciado, gbc);
        gbc.gridy++;

        for (int i = 0; i < 4; i++) {
            txtOpciones[i] = new JTextField();
            agregarCampo(panelFormulario, gbc, LETRAS[i] + ".", txtOpciones[i]);
        }
        agregarCampo(panelFormulario, gbc, "Respuesta correcta:", comboRespuestaCorrecta);
        agregarCampo(panelFormulario, gbc, "Competencia:", comboCompetencia);
        agregarCampo(panelFormulario, gbc, "Tema:", txtTema);
        agregarCampo(panelFormulario, gbc, "Dificultad:", comboDificultad);
        agregarCampo(panelFormulario, gbc, "Recurso multimedia (URL/ruta):", txtRecursoMultimedia);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnGenerar.putClientProperty("JButton.buttonType", "default");
        btnGenerar.addActionListener(e -> generarPregunta());
        panelFormulario.add(btnGenerar, gbc);

        JPanel panelNorte = new JPanel(new BorderLayout(4, 4));
        panelNorte.setOpaque(false);
        panelNorte.add(rol, BorderLayout.NORTH);
        panelNorte.add(new JScrollPane(panelFormulario), BorderLayout.CENTER);

        JPanel panelLista = new JPanel(new BorderLayout());
        panelLista.setBackground(Color.WHITE);
        panelLista.setBorder(tituloSeccion("Preguntas generadas en esta sesión"));
        panelLista.setPreferredSize(new Dimension(0, 140));
        panelLista.add(new JScrollPane(listaGeneradas), BorderLayout.CENTER);

        add(panelNorte, BorderLayout.CENTER);
        add(panelLista, BorderLayout.SOUTH);

        actualizarDisponibilidadRecurso();
    }

    private void actualizarDisponibilidadRecurso() {
        boolean esMultimedia = "Multimedia".equals(comboTipo.getSelectedItem());
        txtRecursoMultimedia.setEnabled(esMultimedia);
        if (!esMultimedia) {
            txtRecursoMultimedia.setText("");
        }
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

    private String tipoSeleccionado() {
        String etiquetaSeleccionada = (String) comboTipo.getSelectedItem();
        for (String[] tipo : TIPOS) {
            if (tipo[0].equals(etiquetaSeleccionada)) {
                return tipo[1];
            }
        }
        throw new IllegalStateException("Tipo de pregunta no reconocido: " + etiquetaSeleccionada);
    }

    private void generarPregunta() {
        List<String> opciones = Arrays.asList(
                txtOpciones[0].getText().trim(), txtOpciones[1].getText().trim(),
                txtOpciones[2].getText().trim(), txtOpciones[3].getText().trim());
        int indiceCorrecta = comboRespuestaCorrecta.getSelectedIndex();
        String textoRespuestaCorrecta = opciones.get(indiceCorrecta);

        QuestionRequest solicitud = new QuestionRequest(
                txtTitulo.getText().trim(),
                txtEnunciado.getText().trim(),
                tipoSeleccionado(),
                ((Competencia) comboCompetencia.getSelectedItem()).toString(),
                txtTema.getText().trim(),
                (Dificultad) comboDificultad.getSelectedItem(),
                opciones,
                textoRespuestaCorrecta,
                txtRecursoMultimedia.getText().trim());

        try {
            Question generada = microkernel.executePlugin(tipoSeleccionado(), solicitud);
            if (generada == null) {
                JOptionPane.showMessageDialog(this,
                        "La solicitud no pasó el pipeline de validación. Revisa que:\n"
                                + "- el título y el enunciado no estén vacíos,\n"
                                + "- las 4 opciones estén completas,\n"
                                + "- la competencia sea una de las de la lista,\n"
                                + "- la respuesta correcta esté entre las opciones.",
                        "No se pudo generar la pregunta", JOptionPane.WARNING_MESSAGE);
                return;
            }
            modeloGeneradas.addElement(generada);
            limpiarFormulario();
            JOptionPane.showMessageDialog(this,
                    "Pregunta " + generada.getId() + " creada y enviada a revisión.",
                    "Pregunta generada", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo generar la pregunta",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        txtTitulo.setText("");
        txtEnunciado.setText("");
        for (JTextField campo : txtOpciones) {
            campo.setText("");
        }
        txtRecursoMultimedia.setText("");
        txtTitulo.requestFocusInWindow();
    }
}
