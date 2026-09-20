package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Pagina;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vista (MVC) del listado "Mis preguntas" del Autor (HU-03): tabla paginada
 * con filtros por estado, competencia y texto, y el estado de cada pregunta
 * con su color. No decide nada: le avisa al {@link MisPreguntasController} lo
 * que hace el usuario y pinta la página que este le entrega.
 */
public class PanelMisPreguntas extends JPanel implements MisPreguntasVista {

    private static final Color GRIS_TEXTO = new Color(0x475569);
    private static final String[] COLUMNAS = {"Id", "Nombre", "Competencia", "Tema", "Dificultad", "Estado"};
    private static final int COLUMNA_ESTADO = 5;
    private static final Integer[] TAMANOS = {5, 10, 20};

    private final MisPreguntasController controlador;
    private final Consumer<String> alAbrir;

    private final JComboBox<EstadoPregunta> comboEstado = comboConTodos(EstadoPregunta.values(), "Todos los estados");
    private final JComboBox<Competencia> comboCompetencia = comboConTodos(Competencia.values(), "Todas las competencias");
    private final JTextField txtBuscar = new JTextField(14);
    private final JButton btnFiltrar = new JButton("Filtrar");
    private final JButton btnLimpiar = new JButton("Limpiar");

    private final ModeloTabla modelo = new ModeloTabla();
    private final JTable tabla = new JTable(modelo);

    private final JComboBox<Integer> comboTamano = new JComboBox<>(TAMANOS);
    private final JButton btnAnterior = new JButton("‹ Anterior");
    private final JButton btnSiguiente = new JButton("Siguiente ›");
    private final JLabel lblPagina = new JLabel(" ");
    private final JButton btnAbrir = new JButton("Abrir seleccionada");
    private final JButton btnNueva = new JButton("Nueva pregunta");

    /**
     * @param alAbrir se invoca con el id de la pregunta que el usuario quiere ver o editar
     * @param alCrear se invoca cuando el usuario quiere redactar una pregunta nueva
     */
    public PanelMisPreguntas(QuestionService servicio, String autor, Consumer<String> alAbrir, Runnable alCrear) {
        this.alAbrir = alAbrir;
        this.controlador = new MisPreguntasController(servicio, autor, this);
        construir(alCrear);
        controlador.iniciar();
    }

    /** Vuelve a consultar la página actual (después de guardar o enviar una pregunta). */
    public void refrescar() {
        controlador.refrescar();
    }

    /** Marca la fila de la pregunta con ese id, si está en la página que se ve. */
    public void seleccionar(String id) {
        int fila = modelo.filaDe(id);
        if (fila >= 0) {
            tabla.setRowSelectionInterval(fila, fila);
        } else {
            tabla.clearSelection();
        }
    }

    @Override
    public void mostrar(Pagina<Question> pagina) {
        modelo.cambiar(pagina.elementos());
        btnAnterior.setEnabled(pagina.hayAnterior());
        btnSiguiente.setEnabled(pagina.haySiguiente());
        lblPagina.setText(pagina.totalElementos() == 0
                ? "Sin resultados"
                : "Página " + pagina.numero() + " de " + pagina.totalPaginas()
                + " · " + pagina.totalElementos() + (pagina.totalElementos() == 1 ? " pregunta" : " preguntas"));
        btnAbrir.setEnabled(false);
    }

    private void construir(Runnable alCrear) {
        setOpaque(false);
        setLayout(new BorderLayout(6, 6));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filtros.setOpaque(false);
        filtros.add(comboEstado);
        filtros.add(comboCompetencia);
        filtros.add(etiqueta("Buscar:"));
        filtros.add(txtBuscar);
        filtros.add(btnFiltrar);
        filtros.add(btnLimpiar);
        txtBuscar.putClientProperty("JTextField.placeholderText", "nombre, tema o pregunta");
        btnFiltrar.addActionListener(e -> filtrar());
        txtBuscar.addActionListener(e -> filtrar());
        btnLimpiar.addActionListener(e -> limpiar());

        tabla.setRowHeight(24);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(60);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(190);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(COLUMNA_ESTADO).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(COLUMNA_ESTADO).setCellRenderer(new RenderizadorEstado());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                btnAbrir.setEnabled(tabla.getSelectedRow() >= 0);
            }
        });
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0) {
                    abrirSeleccionada();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0)));
        scroll.setPreferredSize(new Dimension(700, 24 * 5 + 30));

        comboTamano.addActionListener(e -> controlador.cambiarTamanoDePagina((Integer) comboTamano.getSelectedItem()));
        btnAnterior.addActionListener(e -> controlador.paginaAnterior());
        btnSiguiente.addActionListener(e -> controlador.paginaSiguiente());
        btnAbrir.addActionListener(e -> abrirSeleccionada());
        btnAbrir.setEnabled(false);
        btnNueva.addActionListener(e -> alCrear.run());
        lblPagina.setForeground(GRIS_TEXTO);

        JPanel paginacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        paginacion.setOpaque(false);
        paginacion.add(etiqueta("Por página:"));
        paginacion.add(comboTamano);
        paginacion.add(btnAnterior);
        paginacion.add(lblPagina);
        paginacion.add(btnSiguiente);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        acciones.setOpaque(false);
        acciones.add(btnAbrir);
        acciones.add(btnNueva);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(paginacion, BorderLayout.WEST);
        pie.add(acciones, BorderLayout.EAST);

        add(filtros, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
    }

    private void filtrar() {
        controlador.filtrar((EstadoPregunta) comboEstado.getSelectedItem(),
                (Competencia) comboCompetencia.getSelectedItem(), txtBuscar.getText());
    }

    private void limpiar() {
        comboEstado.setSelectedItem(null);
        comboCompetencia.setSelectedItem(null);
        txtBuscar.setText("");
        controlador.limpiarFiltros();
    }

    private void abrirSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila >= 0) {
            alAbrir.accept(modelo.preguntaEn(fila).getId());
        }
    }

    private static JLabel etiqueta(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(GRIS_TEXTO);
        return label;
    }

    /** Combo cuyo primer elemento ({@code null}) significa "sin filtro" y se muestra con el texto dado. */
    private static <T> JComboBox<T> comboConTodos(T[] valores, String textoTodos) {
        DefaultComboBoxModel<T> modeloCombo = new DefaultComboBoxModel<>();
        modeloCombo.addElement(null);
        for (T valor : valores) {
            modeloCombo.addElement(valor);
        }
        JComboBox<T> combo = new JComboBox<>(modeloCombo);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value == null ? textoTodos : value,
                        index, isSelected, cellHasFocus);
            }
        });
        return combo;
    }

    /** Modelo de la tabla: las preguntas de la página que se está viendo. */
    private static final class ModeloTabla extends AbstractTableModel {

        private List<Question> preguntas = new ArrayList<>();

        void cambiar(List<Question> nuevas) {
            preguntas = new ArrayList<>(nuevas);
            fireTableDataChanged();
        }

        Question preguntaEn(int fila) {
            return preguntas.get(fila);
        }

        int filaDe(String id) {
            for (int i = 0; i < preguntas.size(); i++) {
                if (preguntas.get(i).getId().equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getRowCount() {
            return preguntas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Question pregunta = preguntas.get(fila);
            return switch (columna) {
                case 0 -> pregunta.getId();
                case 1 -> pregunta.getNombre();
                case 2 -> pregunta.getCompetencia();
                case 3 -> pregunta.getTema();
                case 4 -> pregunta.getDificultad();
                default -> pregunta.getEstado();
            };
        }
    }

    /** Pinta el estado de cada pregunta como una celda de color (RF-14: los estados se ven con colores). */
    private static final class RenderizadorEstado extends DefaultTableCellRenderer {

        RenderizadorEstado() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(0, 6, 0, 6));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            if (value instanceof EstadoPregunta estado) {
                setBackground(EstadoColores.de(estado));
                setForeground(Color.WHITE);
                setFont(getFont().deriveFont(Font.BOLD));
            }
            return this;
        }
    }
}
