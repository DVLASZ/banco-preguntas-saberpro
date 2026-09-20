package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import co.unicauca.saberpro.preguntas.domain.validation.QuestionValidationException;
import co.unicauca.saberpro.preguntas.domain.validation.Violacion;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventana del rol <b>Autor de preguntas</b>: redacta sus preguntas (HU-01),
 * las guarda como {@code BORRADOR} y las envía a revisión (HU-02). Solo
 * puede modificar una pregunta mientras esté en Borrador (RF-06); en
 * cualquier otro estado el formulario queda de solo lectura.
 *
 * <p>Al guardar o enviar se aplica la validación estructural (HU03): si algo
 * incumple, no se guarda nada y los campos afectados se resaltan en rojo.
 * Guardar y enviar a revisión son acciones distintas; enviar pide
 * confirmación, y cancelar pide confirmación si hay cambios sin guardar.
 */
public class GUIQuestions extends JFrame {

    private static final Color GRIS_TEXTO = new Color(0x475569);
    private static final Color FONDO_CAMPO = new Color(0xF1F5F9);
    private static final Color ROJO_ERROR = new Color(0xDC2626);
    private static final String SIN_SELECCION = "— Seleccione —";

    private final QuestionService service;
    private final String usuario;

    private PanelMisPreguntas panelMisPreguntas;
    private final JButton btnGuardar = new JButton("Guardar borrador");
    private final JButton btnEnviar = new JButton("Enviar a revisión");
    private final JButton btnCancelar = new JButton("Cancelar");

    private final JTextField txtId = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JTextArea txtContexto = new JTextArea(3, 30);
    private final JTextArea txtEnunciado = new JTextArea(2, 30);
    private final JTextField txtOpcionA = new JTextField();
    private final JTextField txtOpcionB = new JTextField();
    private final JTextField txtOpcionC = new JTextField();
    private final JTextField txtOpcionD = new JTextField();
    private final JComboBox<String> comboRespuesta = new JComboBox<>(new String[]{"", "A", "B", "C", "D"});
    private final JTextArea txtJustificacion = new JTextArea(3, 30);
    private final JTextArea txtBibliografia = new JTextArea(2, 30);
    private final JComboBox<Competencia> comboCompetencia = comboConSeleccion(Competencia.values());
    private final JTextField txtTema = new JTextField();
    private final JTextField txtSubtema = new JTextField();
    private final JComboBox<Dificultad> comboDificultad = comboConSeleccion(Dificultad.values());
    private final EstadoBadge badgeEstadoActual = new EstadoBadge();
    private final JLabel lblAviso = new JLabel(" ");

    /** Componente de la interfaz que corresponde a cada campo de la validación, para resaltarlo. */
    private final Map<String, JComponent> componentesPorCampo = new HashMap<>();
    private final Map<JComponent, Border> bordesOriginales = new HashMap<>();

    /** {@code null} mientras se redacta una pregunta nueva aún no guardada. */
    private String idPreguntaEnEdicion;
    private boolean edicionPermitida;
    /** Contenido tal como se cargó o se guardó, para detectar cambios sin guardar. */
    private ContenidoPregunta instantanea;

    public GUIQuestions(QuestionService service, String usuario) {
        super("Banco de Preguntas Saber Pro - Autor de Preguntas");
        this.service = service;
        this.usuario = usuario;
        construirInterfaz();
        limpiarFormulario();
        habilitarFormulario(false, "Seleccione una pregunta o cree una nueva.");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(820, 940);
        setLocationRelativeTo(null);
    }

    private static <T> JComboBox<T> comboConSeleccion(T[] valores) {
        DefaultComboBoxModel<T> modelo = new DefaultComboBoxModel<>();
        modelo.addElement(null);
        for (T valor : valores) {
            modelo.addElement(valor);
        }
        JComboBox<T> combo = new JComboBox<>(modelo);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value == null ? SIN_SELECCION : value,
                        index, isSelected, cellHasFocus);
            }
        });
        return combo;
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel rol = new JLabel("Rol: Autor de preguntas — usuario: " + usuario);
        rol.setForeground(GRIS_TEXTO);
        rol.setFont(rol.getFont().deriveFont(Font.ITALIC, 12f));

        // HU-03: listado paginado y filtrable de las preguntas del autor.
        panelMisPreguntas = new PanelMisPreguntas(service, usuario, this::abrirPregunta, this::iniciarPreguntaNueva);
        JPanel panelSeleccion = new JPanel(new BorderLayout());
        panelSeleccion.setBackground(Color.WHITE);
        panelSeleccion.setBorder(BorderFactory.createCompoundBorder(tituloSeccion("Mis preguntas"),
                new EmptyBorder(4, 6, 6, 6)));
        panelSeleccion.add(panelMisPreguntas, BorderLayout.CENTER);

        JPanel panelNorte = new JPanel(new BorderLayout(4, 4));
        panelNorte.setOpaque(false);
        panelNorte.add(rol, BorderLayout.NORTH);
        panelNorte.add(panelSeleccion, BorderLayout.CENTER);

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBackground(Color.WHITE);
        panelFormulario.setBorder(tituloSeccion("Redacción de la pregunta"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        txtId.setEditable(false);
        txtId.setBackground(FONDO_CAMPO);
        agregarCampo(panelFormulario, gbc, "Id:", txtId, null);
        agregarCampo(panelFormulario, gbc, "Nombre:", txtNombre, "nombre");
        agregarArea(panelFormulario, gbc, "Contexto:", txtContexto, "contexto");
        agregarArea(panelFormulario, gbc, "Pregunta directa:", txtEnunciado, "enunciado");
        agregarCampo(panelFormulario, gbc, "Opción A:", txtOpcionA, "opcionA");
        agregarCampo(panelFormulario, gbc, "Opción B:", txtOpcionB, "opcionB");
        agregarCampo(panelFormulario, gbc, "Opción C:", txtOpcionC, "opcionC");
        agregarCampo(panelFormulario, gbc, "Opción D:", txtOpcionD, "opcionD");
        agregarCampo(panelFormulario, gbc, "Respuesta correcta:", comboRespuesta, "respuestaCorrecta");
        agregarArea(panelFormulario, gbc, "Justificación:", txtJustificacion, "justificacion");
        agregarArea(panelFormulario, gbc, "Bibliografía:", txtBibliografia, "bibliografia");
        agregarCampo(panelFormulario, gbc, "Competencia:", comboCompetencia, "competencia");
        agregarCampo(panelFormulario, gbc, "Tema:", txtTema, "tema");
        agregarCampo(panelFormulario, gbc, "Subtema:", txtSubtema, "subtema");
        agregarCampo(panelFormulario, gbc, "Dificultad:", comboDificultad, "dificultad");

        gbc.gridx = 0;
        gbc.weightx = 0;
        panelFormulario.add(etiqueta("Estado actual:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel envolturaBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        envolturaBadge.setOpaque(false);
        envolturaBadge.add(badgeEstadoActual);
        panelFormulario.add(envolturaBadge, gbc);

        JScrollPane scrollFormulario = new JScrollPane(panelFormulario);
        scrollFormulario.setBorder(BorderFactory.createEmptyBorder());
        scrollFormulario.getVerticalScrollBar().setUnitIncrement(16);

        lblAviso.setForeground(GRIS_TEXTO);
        lblAviso.setFont(lblAviso.getFont().deriveFont(Font.ITALIC, 12f));
        btnGuardar.putClientProperty("JButton.buttonType", "default");
        btnGuardar.addActionListener(e -> guardarBorrador());
        btnEnviar.addActionListener(e -> enviarARevision());
        btnCancelar.addActionListener(e -> cancelar());

        JPanel panelAcciones = new JPanel(new BorderLayout(8, 0));
        panelAcciones.setOpaque(false);
        panelAcciones.add(lblAviso, BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.setOpaque(false);
        botones.add(btnCancelar);
        botones.add(btnEnviar);
        botones.add(btnGuardar);
        panelAcciones.add(botones, BorderLayout.EAST);

        add(panelNorte, BorderLayout.NORTH);
        add(scrollFormulario, BorderLayout.CENTER);
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

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, String etiqueta, JComponent campo, String nombreCampo) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(etiqueta(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(campo, gbc);
        gbc.gridy++;
        if (nombreCampo != null) {
            registrarCampo(nombreCampo, campo);
        }
    }

    private void agregarArea(JPanel panel, GridBagConstraints gbc, String etiqueta, JTextArea area, String nombreCampo) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(4, 6, 4, 6));
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
        registrarCampo(nombreCampo, scroll);
    }

    private void registrarCampo(String nombreCampo, JComponent componente) {
        componentesPorCampo.put(nombreCampo, componente);
        bordesOriginales.put(componente, componente.getBorder());
    }

    // ---- selección y carga ----

    /** Abre una pregunta del listado en el formulario (si hay cambios sin guardar, pregunta antes). */
    private void abrirPregunta(String id) {
        if (!confirmarDescarteDeCambios()) {
            panelMisPreguntas.seleccionar(idPreguntaEnEdicion);
            return;
        }
        mostrarPregunta(service.obtenerPregunta(id));
    }

    private void mostrarPregunta(Question pregunta) {
        limpiarResaltados();
        idPreguntaEnEdicion = pregunta.getId();
        panelMisPreguntas.seleccionar(pregunta.getId());
        txtId.setText(pregunta.getId());
        cargarFormulario(ContenidoPregunta.de(pregunta));
        badgeEstadoActual.mostrar(pregunta.getEstado());
        boolean editable = pregunta.getEstado() == EstadoPregunta.BORRADOR;
        habilitarFormulario(editable, editable ? "Puede modificarla mientras esté en Borrador."
                : "Solo lectura: la pregunta está " + pregunta.getEstado() + ".");
    }

    private void iniciarPreguntaNueva() {
        if (!confirmarDescarteDeCambios()) {
            return;
        }
        limpiarResaltados();
        panelMisPreguntas.seleccionar(null);
        limpiarFormulario();
        habilitarFormulario(true, "Complete todos los campos y guarde el borrador.");
        txtNombre.requestFocusInWindow();
    }

    private void limpiarFormulario() {
        idPreguntaEnEdicion = null;
        txtId.setText("(nueva)");
        cargarFormulario(new ContenidoPregunta("", "", "", "", "", "", "", "", "", "", null, "", "", null));
        badgeEstadoActual.setText("");
    }

    private void cargarFormulario(ContenidoPregunta c) {
        txtNombre.setText(c.nombre());
        txtContexto.setText(c.contexto());
        txtEnunciado.setText(c.enunciado());
        txtOpcionA.setText(c.opcionA());
        txtOpcionB.setText(c.opcionB());
        txtOpcionC.setText(c.opcionC());
        txtOpcionD.setText(c.opcionD());
        comboRespuesta.setSelectedItem(c.respuestaCorrecta());
        txtJustificacion.setText(c.justificacion());
        txtBibliografia.setText(c.bibliografia());
        comboCompetencia.setSelectedItem(c.competencia());
        txtTema.setText(c.tema());
        txtSubtema.setText(c.subtema());
        comboDificultad.setSelectedItem(c.dificultad());
        instantanea = leerFormulario();
    }

    private ContenidoPregunta leerFormulario() {
        return new ContenidoPregunta(
                txtNombre.getText(), txtContexto.getText(), txtEnunciado.getText(),
                txtOpcionA.getText(), txtOpcionB.getText(), txtOpcionC.getText(), txtOpcionD.getText(),
                (String) comboRespuesta.getSelectedItem(), txtJustificacion.getText(), txtBibliografia.getText(),
                (Competencia) comboCompetencia.getSelectedItem(), txtTema.getText(), txtSubtema.getText(),
                (Dificultad) comboDificultad.getSelectedItem());
    }

    // ---- acciones ----

    /** HU-01: guarda (o actualiza) el borrador aplicando la validación estructural. */
    private void guardarBorrador() {
        if (guardar()) {
            JOptionPane.showMessageDialog(this,
                    "Pregunta " + idPreguntaEnEdicion + " guardada como Borrador.",
                    "Borrador guardado", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** HU-02: tras confirmar, guarda los cambios pendientes y envía la pregunta a revisión. */
    private void enviarARevision() {
        int decision = JOptionPane.showConfirmDialog(this,
                "¿Enviar la pregunta a revisión?\nUna vez enviada ya no podrá modificarla.",
                "Enviar a revisión", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (decision != JOptionPane.YES_OPTION) {
            return;
        }
        if (!guardar()) {
            return;
        }
        try {
            service.enviarARevision(idPreguntaEnEdicion, usuario);
        } catch (QuestionValidationException ex) {
            mostrarViolaciones(ex);
            return;
        } catch (RuntimeException ex) {
            mostrarError("No se pudo enviar a revisión", ex.getMessage());
            return;
        }
        String id = idPreguntaEnEdicion;
        JOptionPane.showMessageDialog(this,
                "Pregunta " + id + " enviada a revisión: quedó en estado Pendiente de revisión.",
                "Pregunta enviada", JOptionPane.INFORMATION_MESSAGE);
        recargarDespuesDeGuardar(id);
    }

    /** @return {@code true} si la pregunta quedó guardada; si no, ya mostró qué corregir */
    private boolean guardar() {
        limpiarResaltados();
        ContenidoPregunta contenido = leerFormulario();
        try {
            if (idPreguntaEnEdicion == null) {
                idPreguntaEnEdicion = service.crearBorrador(contenido, usuario).getId();
            } else {
                service.actualizarContenido(idPreguntaEnEdicion, contenido, usuario);
            }
        } catch (QuestionValidationException ex) {
            mostrarViolaciones(ex);
            return false;
        } catch (RuntimeException ex) {
            mostrarError("No se pudo guardar la pregunta", ex.getMessage());
            return false;
        }
        recargarDespuesDeGuardar(idPreguntaEnEdicion);
        return true;
    }

    private void recargarDespuesDeGuardar(String id) {
        panelMisPreguntas.refrescar();
        mostrarPregunta(service.obtenerPregunta(id));
    }

    /** Cancela la edición: si hay cambios sin guardar pide confirmación antes de descartarlos. */
    private void cancelar() {
        if (!confirmarDescarteDeCambios()) {
            return;
        }
        limpiarResaltados();
        if (idPreguntaEnEdicion == null) {
            limpiarFormulario();
            habilitarFormulario(false, "Seleccione una pregunta o cree una nueva.");
        } else {
            mostrarPregunta(service.obtenerPregunta(idPreguntaEnEdicion));
        }
    }

    private boolean confirmarDescarteDeCambios() {
        if (!edicionPermitida || leerFormulario().equals(instantanea)) {
            return true;
        }
        int decision = JOptionPane.showConfirmDialog(this,
                "Hay cambios sin guardar. ¿Desea descartarlos?",
                "Descartar cambios", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return decision == JOptionPane.YES_OPTION;
    }

    // ---- errores y resaltado ----

    private void mostrarViolaciones(QuestionValidationException ex) {
        resaltar(ex.getViolaciones());
        StringBuilder mensaje = new StringBuilder("La pregunta no cumple la validación estructural.\n"
                + "Corrija los campos resaltados en rojo:\n");
        for (Violacion violacion : ex.getViolaciones()) {
            mensaje.append("\n  • ").append(violacion.mensaje());
        }
        JOptionPane.showMessageDialog(this, mensaje.toString(), "No se pudo guardar la pregunta",
                JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    private void resaltar(List<Violacion> violaciones) {
        for (Violacion violacion : violaciones) {
            JComponent componente = componentesPorCampo.get(violacion.campo());
            if (componente != null) {
                componente.setBorder(BorderFactory.createLineBorder(ROJO_ERROR, 2));
            }
        }
    }

    private void limpiarResaltados() {
        bordesOriginales.forEach(JComponent::setBorder);
    }

    // ---- estado del formulario ----

    private void habilitarFormulario(boolean habilitado, String aviso) {
        edicionPermitida = habilitado;
        for (JTextField campo : new JTextField[]{txtNombre, txtOpcionA, txtOpcionB, txtOpcionC, txtOpcionD,
                txtTema, txtSubtema}) {
            campo.setEditable(habilitado);
            campo.setBackground(habilitado ? Color.WHITE : FONDO_CAMPO);
        }
        for (JTextArea area : new JTextArea[]{txtContexto, txtEnunciado, txtJustificacion, txtBibliografia}) {
            area.setEditable(habilitado);
            area.setBackground(habilitado ? Color.WHITE : FONDO_CAMPO);
        }
        comboRespuesta.setEnabled(habilitado);
        comboCompetencia.setEnabled(habilitado);
        comboDificultad.setEnabled(habilitado);
        btnGuardar.setEnabled(habilitado);
        btnEnviar.setEnabled(habilitado);
        btnCancelar.setEnabled(habilitado);
        lblAviso.setText(aviso);
    }
}
