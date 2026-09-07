package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import co.unicauca.saberpro.preguntas.infra.Observer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vista de estadísticas: muestra cuántas preguntas hay en cada estado del
 * ciclo de vida (RF-14). Se suscribe como {@link Observer} de
 * {@link QuestionService} y se refresca automáticamente cada vez que una
 * pregunta cambia de estado.
 */
public class GUIObserver1 extends JFrame implements Observer {

    private final QuestionService service;
    private final Map<EstadoPregunta, JLabel> etiquetasConteo = new LinkedHashMap<>();
    private final JLabel lblTotal = new JLabel();

    public GUIObserver1(QuestionService service) {
        super("Vista de Estadísticas");
        this.service = service;
        construirInterfaz();
        actualizar();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(320, 330);
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        JLabel titulo = new JLabel("Preguntas por estado");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 15f));
        titulo.setBorder(new EmptyBorder(14, 16, 4, 16));
        add(titulo, BorderLayout.NORTH);

        JPanel filas = new JPanel();
        filas.setOpaque(false);
        filas.setLayout(new BoxLayout(filas, BoxLayout.Y_AXIS));
        filas.setBorder(new EmptyBorder(8, 16, 8, 16));

        for (EstadoPregunta estado : EstadoPregunta.values()) {
            filas.add(construirFila(estado));
            filas.add(Box.createVerticalStrut(6));
        }

        add(filas, BorderLayout.CENTER);

        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.PLAIN, 12f));
        lblTotal.setForeground(new Color(0x64748B));
        lblTotal.setBorder(new EmptyBorder(4, 16, 12, 16));
        add(lblTotal, BorderLayout.SOUTH);
    }

    private JPanel construirFila(EstadoPregunta estado) {
        JPanel fila = new JPanel(new BorderLayout(10, 0));
        fila.setOpaque(false);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JPanel punto = new PuntoColor(EstadoColores.de(estado));
        punto.setPreferredSize(new Dimension(12, 12));

        JPanel puntoContenedor = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        puntoContenedor.setOpaque(false);
        puntoContenedor.add(punto);

        JLabel nombre = new JLabel(estado.toString());
        nombre.setFont(nombre.getFont().deriveFont(Font.PLAIN, 13f));

        JLabel conteo = new JLabel("0");
        conteo.setFont(conteo.getFont().deriveFont(Font.BOLD, 13f));
        conteo.setHorizontalAlignment(SwingConstants.RIGHT);
        etiquetasConteo.put(estado, conteo);

        JPanel izquierda = new JPanel(new BorderLayout(8, 0));
        izquierda.setOpaque(false);
        izquierda.add(puntoContenedor, BorderLayout.WEST);
        izquierda.add(nombre, BorderLayout.CENTER);

        fila.add(izquierda, BorderLayout.CENTER);
        fila.add(conteo, BorderLayout.EAST);
        return fila;
    }

    @Override
    public void actualizar() {
        Map<EstadoPregunta, Long> conteo = service.contarPorEstado();
        long total = 0;
        for (EstadoPregunta estado : EstadoPregunta.values()) {
            long cantidad = conteo.getOrDefault(estado, 0L);
            etiquetasConteo.get(estado).setText(String.valueOf(cantidad));
            total += cantidad;
        }
        lblTotal.setText("Total: " + total + " preguntas");
    }

    /** Cuadrito de color sólido usado como indicador visual de estado. */
    private static class PuntoColor extends JPanel {
        PuntoColor(Color color) {
            setOpaque(false);
            setBackground(color);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 4, 4);
            g2.dispose();
        }
    }
}
