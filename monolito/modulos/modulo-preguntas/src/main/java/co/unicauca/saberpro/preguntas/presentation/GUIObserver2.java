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
 * Vista gráfica: muestra un diagrama de pastel con el porcentaje de
 * preguntas en cada estado del ciclo de vida (RF-14). Se suscribe como
 * {@link Observer} de {@link QuestionService} y se redibuja
 * automáticamente cada vez que una pregunta cambia de estado.
 */
public class GUIObserver2 extends JFrame implements Observer {

    private final QuestionService service;
    private final PieChartPanel panelPastel = new PieChartPanel();
    private final Map<EstadoPregunta, JLabel> etiquetasLeyenda = new LinkedHashMap<>();

    public GUIObserver2(QuestionService service) {
        super("Vista Gráfica");
        this.service = service;
        construirInterfaz();
        actualizar();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(380, 520);
    }

    private void construirInterfaz() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(5, 5));

        JLabel titulo = new JLabel("Distribución de preguntas", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 15f));
        titulo.setBorder(new EmptyBorder(14, 16, 4, 16));
        add(titulo, BorderLayout.NORTH);

        panelPastel.setBorder(new EmptyBorder(4, 16, 4, 16));
        add(panelPastel, BorderLayout.CENTER);

        JPanel leyenda = new JPanel();
        leyenda.setOpaque(false);
        leyenda.setLayout(new BoxLayout(leyenda, BoxLayout.Y_AXIS));
        leyenda.setBorder(new EmptyBorder(4, 16, 14, 16));
        for (EstadoPregunta estado : EstadoPregunta.values()) {
            leyenda.add(construirFilaLeyenda(estado));
            leyenda.add(Box.createVerticalStrut(4));
        }
        add(leyenda, BorderLayout.SOUTH);
    }

    private JPanel construirFilaLeyenda(EstadoPregunta estado) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setOpaque(false);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel swatch = new JLabel("■"); // ■
        swatch.setForeground(EstadoColores.de(estado));
        swatch.setFont(swatch.getFont().deriveFont(16f));

        JLabel texto = new JLabel();
        texto.setFont(texto.getFont().deriveFont(Font.PLAIN, 12.5f));
        etiquetasLeyenda.put(estado, texto);

        JPanel izquierda = new JPanel(new BorderLayout(8, 0));
        izquierda.setOpaque(false);
        izquierda.add(swatch, BorderLayout.WEST);
        izquierda.add(texto, BorderLayout.CENTER);

        fila.add(izquierda, BorderLayout.CENTER);
        return fila;
    }

    @Override
    public void actualizar() {
        Map<EstadoPregunta, Long> conteo = service.contarPorEstado();
        long total = conteo.values().stream().mapToLong(Long::longValue).sum();

        for (EstadoPregunta estado : EstadoPregunta.values()) {
            long cantidad = conteo.getOrDefault(estado, 0L);
            double porcentaje = total == 0 ? 0 : (cantidad * 100.0 / total);
            etiquetasLeyenda.get(estado).setText(
                    String.format("%s: %d (%.0f%%)", estado, cantidad, porcentaje));
        }
        panelPastel.actualizarDatos(conteo, total);
    }

    /** Panel que dibuja el diagrama de pastel con AWT (sin dependencias externas). */
    private static class PieChartPanel extends JPanel {
        private Map<EstadoPregunta, Long> conteo = Map.of();
        private long total;

        PieChartPanel() {
            setOpaque(false);
        }

        void actualizarDatos(Map<EstadoPregunta, Long> conteo, long total) {
            this.conteo = conteo;
            this.total = total;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (total == 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int diametro = Math.min(getWidth(), getHeight()) - 40;
            int x = (getWidth() - diametro) / 2;
            int y = (getHeight() - diametro) / 2;

            double anguloInicial = 90;
            for (EstadoPregunta estado : EstadoPregunta.values()) {
                long cantidad = conteo.getOrDefault(estado, 0L);
                if (cantidad == 0) {
                    continue;
                }
                double anguloBarrido = cantidad * 360.0 / total;
                g2.setColor(EstadoColores.de(estado));
                g2.fillArc(x, y, diametro, diametro, (int) Math.round(anguloInicial),
                        -(int) Math.round(anguloBarrido));
                anguloInicial -= anguloBarrido;
            }
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(x, y, diametro, diametro);
        }
    }
}
