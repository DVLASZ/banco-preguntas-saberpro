package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

/**
 * Paleta de colores semántica para el ciclo de vida de una pregunta
 * (RF-14), compartida por {@link GUIObserver1} y {@link GUIObserver2}
 * para que la vista de estadísticas y la vista gráfica sean visualmente
 * consistentes entre sí (y con los mockups del taller).
 */
final class EstadoColores {

    private static final Map<EstadoPregunta, Color> PALETA = new EnumMap<>(EstadoPregunta.class);
    static {
        PALETA.put(EstadoPregunta.BORRADOR, new Color(0x94A3B8));           // gris: aún no enviada
        PALETA.put(EstadoPregunta.PENDIENTE_REVISION, new Color(0xF59E0B)); // ámbar: en cola
        PALETA.put(EstadoPregunta.EN_REVISION, new Color(0x3B82F6));        // azul: revisión activa
        PALETA.put(EstadoPregunta.APROBADA, new Color(0x14B8A6));          // teal: visto bueno
        PALETA.put(EstadoPregunta.RECHAZADA, new Color(0xEF4444));         // rojo: rechazada
        PALETA.put(EstadoPregunta.PUBLICADA, new Color(0x22C55E));         // verde: estado final positivo
        PALETA.put(EstadoPregunta.ARCHIVADA, new Color(0x475569));         // pizarra: inactiva
    }

    private EstadoColores() {
    }

    static Color de(EstadoPregunta estado) {
        return PALETA.get(estado);
    }
}
