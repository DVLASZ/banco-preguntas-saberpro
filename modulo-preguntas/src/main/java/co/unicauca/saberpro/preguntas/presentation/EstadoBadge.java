package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Insignia ("chip") de color sólido que muestra el estado de una
 * pregunta, en vez de un simple texto plano. Usa la misma paleta que
 * {@link GUIObserver1} y {@link GUIObserver2} para que el estado se
 * reconozca por color en toda la aplicación.
 */
class EstadoBadge extends JLabel {

    EstadoBadge() {
        setOpaque(false);
        setForeground(Color.WHITE);
        setHorizontalAlignment(SwingConstants.CENTER);
        setFont(getFont().deriveFont(Font.BOLD, 12f));
        setBorder(new EmptyBorder(3, 10, 3, 10));
    }

    void mostrar(EstadoPregunta estado) {
        setText(estado.toString());
        setBackground(EstadoColores.de(estado));
        setPreferredSize(null);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getText() != null && !getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
        }
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension base = super.getPreferredSize();
        return new Dimension(base.width, Math.max(base.height, 22));
    }
}
