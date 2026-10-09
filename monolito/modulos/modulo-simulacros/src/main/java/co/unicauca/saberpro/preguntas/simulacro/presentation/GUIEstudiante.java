package co.unicauca.saberpro.preguntas.simulacro.presentation;

import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.simulacro.domain.IntentoSimulacro;
import co.unicauca.saberpro.preguntas.simulacro.domain.Simulacro;
import co.unicauca.saberpro.preguntas.simulacro.domain.SimulacroService;
import co.unicauca.saberpro.usuarios.domain.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana del rol <b>Estudiante</b>: elige un simulacro disponible y lo
 * presenta dentro de su tiempo máximo (HU-13, RF-23), con calificación
 * automática al finalizar (HU-14, RF-24). Solo conoce
 * {@link SimulacroService} — no sabe cómo se generó el simulacro ni cómo
 * se persisten los intentos (DIP).
 */
public class GUIEstudiante extends JFrame {

    private static final Color GRIS_TEXTO = new Color(0x475569);

    private final SimulacroService service;
    private final User usuario;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelCentral = new JPanel();

    // --- Tarjeta de selección ---
    private final JComboBox<Simulacro> comboSimulacros = new JComboBox<>();
    private final JButton btnComenzar = new JButton("Comenzar");

    // --- Tarjeta de presentación ---
    private final JLabel lblProgreso = new JLabel();
    private final JLabel lblTiempoRestante = new JLabel();
    private final JTextArea txtEnunciado = new JTextArea(4, 30);
    private final JRadioButton[] radiosOpciones = new JRadioButton[4];
    private final ButtonGroup grupoOpciones = new ButtonGroup();
    private final JButton btnAnterior = new JButton("Anterior");
    private final JButton btnSiguiente = new JButton("Siguiente");
    private final JButton btnFinalizar = new JButton("Finalizar");

    private Simulacro simulacroActual;
    private IntentoSimulacro intentoActual;
    private int indicePreguntaActual;
    private int segundosRestantes;
    private Timer cronometro;

    public GUIEstudiante(User usuario, SimulacroService service) {
        super("Banco de Preguntas Saber Pro - Estudiante");
        this.usuario = usuario;
        this.service = service;
        construirInterfaz();
        cargarSimulacrosDisponibles();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 560);
        setLocationRelativeTo(null);

        // Si el estudiante cierra la ventana a mitad de un simulacro, el
        // cronómetro no debe seguir corriendo en segundo plano.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (cronometro != null) {
                    cronometro.stop();
                }
            }
        });
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel rol = new JLabel("Rol: Estudiante");
        rol.setForeground(GRIS_TEXTO);
        rol.setFont(rol.getFont().deriveFont(Font.ITALIC, 12f));
        add(rol, BorderLayout.NORTH);

        panelCentral.setLayout(cardLayout);
        panelCentral.setOpaque(false);
        panelCentral.add(construirTarjetaSeleccion(), "seleccion");
        panelCentral.add(construirTarjetaPresentacion(), "presentacion");
        add(panelCentral, BorderLayout.CENTER);

        cardLayout.show(panelCentral, "seleccion");
    }

    private JPanel construirTarjetaSeleccion() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titulo = new JLabel("Elige un simulacro para presentar");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(titulo, gbc);

        gbc.gridy++;
        comboSimulacros.setPreferredSize(new Dimension(320, 26));
        panel.add(comboSimulacros, gbc);

        gbc.gridy++;
        btnComenzar.putClientProperty("JButton.buttonType", "default");
        btnComenzar.addActionListener(e -> comenzarSimulacro());
        panel.add(btnComenzar, gbc);

        return panel;
    }

    private JPanel construirTarjetaPresentacion() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setOpaque(false);
        lblProgreso.setFont(lblProgreso.getFont().deriveFont(Font.BOLD, 13f));
        lblTiempoRestante.setForeground(new Color(0xEF4444));
        lblTiempoRestante.setFont(lblTiempoRestante.getFont().deriveFont(Font.BOLD, 13f));
        panelSuperior.add(lblProgreso, BorderLayout.WEST);
        panelSuperior.add(lblTiempoRestante, BorderLayout.EAST);

        txtEnunciado.setLineWrap(true);
        txtEnunciado.setWrapStyleWord(true);
        txtEnunciado.setEditable(false);
        txtEnunciado.setOpaque(false);
        txtEnunciado.setFont(txtEnunciado.getFont().deriveFont(14f));

        JPanel panelOpciones = new JPanel();
        panelOpciones.setOpaque(false);
        panelOpciones.setLayout(new BoxLayout(panelOpciones, BoxLayout.Y_AXIS));
        String[] letras = {"A", "B", "C", "D"};
        for (int i = 0; i < radiosOpciones.length; i++) {
            radiosOpciones[i] = new JRadioButton();
            radiosOpciones[i].setOpaque(false);
            char letra = letras[i].charAt(0);
            radiosOpciones[i].addActionListener(e -> seleccionarRespuesta(letra));
            grupoOpciones.add(radiosOpciones[i]);
            panelOpciones.add(radiosOpciones[i]);
            panelOpciones.add(Box.createVerticalStrut(4));
        }

        JPanel panelCentroPresentacion = new JPanel(new BorderLayout(10, 10));
        panelCentroPresentacion.setOpaque(false);
        panelCentroPresentacion.add(txtEnunciado, BorderLayout.NORTH);
        panelCentroPresentacion.add(panelOpciones, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panelBotones.setOpaque(false);
        btnAnterior.addActionListener(e -> irAPregunta(indicePreguntaActual - 1));
        btnSiguiente.addActionListener(e -> irAPregunta(indicePreguntaActual + 1));
        btnFinalizar.putClientProperty("JButton.buttonType", "default");
        btnFinalizar.addActionListener(e -> confirmarFinalizar());
        panelBotones.add(btnAnterior);
        panelBotones.add(btnSiguiente);
        panelBotones.add(btnFinalizar);

        panel.add(panelSuperior, BorderLayout.NORTH);
        panel.add(panelCentroPresentacion, BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.SOUTH);
        return panel;
    }

    private void cargarSimulacrosDisponibles() {
        comboSimulacros.removeAllItems();
        for (Simulacro simulacro : service.listarSimulacros()) {
            comboSimulacros.addItem(simulacro);
        }
        btnComenzar.setEnabled(comboSimulacros.getItemCount() > 0);
    }

    private void comenzarSimulacro() {
        Simulacro seleccionado = (Simulacro) comboSimulacros.getSelectedItem();
        if (seleccionado == null) {
            return;
        }
        simulacroActual = seleccionado;
        intentoActual = service.iniciarIntento(simulacroActual.getId(), usuario.getUsername());
        indicePreguntaActual = 0;
        segundosRestantes = simulacroActual.getTiempoMaximoMinutos() * 60;

        iniciarCronometro();
        mostrarPreguntaActual();
        cardLayout.show(panelCentral, "presentacion");
    }

    private void iniciarCronometro() {
        if (cronometro != null) {
            cronometro.stop();
        }
        actualizarEtiquetaTiempo();
        cronometro = new Timer(1000, e -> {
            segundosRestantes--;
            actualizarEtiquetaTiempo();
            if (segundosRestantes <= 0) {
                finalizarSimulacro();
            }
        });
        cronometro.start();
    }

    private void actualizarEtiquetaTiempo() {
        int minutos = Math.max(0, segundosRestantes) / 60;
        int segundos = Math.max(0, segundosRestantes) % 60;
        lblTiempoRestante.setText(String.format("Tiempo restante: %02d:%02d", minutos, segundos));
    }

    private void mostrarPreguntaActual() {
        Question pregunta = simulacroActual.getPreguntas().get(indicePreguntaActual);
        lblProgreso.setText("Pregunta " + (indicePreguntaActual + 1) + " de " + simulacroActual.getPreguntas().size());
        txtEnunciado.setText(pregunta.getEnunciado());

        String[] textos = {
                "A. " + pregunta.getOpciones().getOpcionA(),
                "B. " + pregunta.getOpciones().getOpcionB(),
                "C. " + pregunta.getOpciones().getOpcionC(),
                "D. " + pregunta.getOpciones().getOpcionD(),
        };
        Character respuestaGuardada = intentoActual.getRespuesta(pregunta.getId());
        grupoOpciones.clearSelection();
        for (int i = 0; i < radiosOpciones.length; i++) {
            radiosOpciones[i].setText(textos[i]);
            char letra = (char) ('A' + i);
            radiosOpciones[i].setSelected(respuestaGuardada != null && respuestaGuardada == letra);
        }

        btnAnterior.setEnabled(indicePreguntaActual > 0);
        btnSiguiente.setEnabled(indicePreguntaActual < simulacroActual.getPreguntas().size() - 1);
    }

    private void seleccionarRespuesta(char letra) {
        Question pregunta = simulacroActual.getPreguntas().get(indicePreguntaActual);
        service.responder(intentoActual.getId(), pregunta.getId(), letra);
    }

    private void irAPregunta(int nuevoIndice) {
        if (nuevoIndice < 0 || nuevoIndice >= simulacroActual.getPreguntas().size()) {
            return;
        }
        indicePreguntaActual = nuevoIndice;
        mostrarPreguntaActual();
    }

    private void confirmarFinalizar() {
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Deseas finalizar el simulacro? No podrás cambiar tus respuestas después.",
                "Finalizar simulacro", JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            finalizarSimulacro();
        }
    }

    private void finalizarSimulacro() {
        if (cronometro != null) {
            cronometro.stop();
        }
        IntentoSimulacro resultado = service.finalizar(intentoActual.getId());
        int total = simulacroActual.getPreguntas().size();
        int aciertos = resultado.getAciertos();
        JOptionPane.showMessageDialog(this,
                "Simulacro finalizado.\nRespuestas correctas: " + aciertos + " de " + total
                        + " (" + Math.round(aciertos * 100.0 / total) + "%).",
                "Resultado del simulacro", JOptionPane.INFORMATION_MESSAGE);

        simulacroActual = null;
        intentoActual = null;
        cargarSimulacrosDisponibles();
        cardLayout.show(panelCentral, "seleccion");
    }
}
