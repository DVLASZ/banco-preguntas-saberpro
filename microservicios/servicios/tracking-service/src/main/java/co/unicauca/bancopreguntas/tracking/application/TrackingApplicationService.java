package co.unicauca.bancopreguntas.tracking.application;

import co.unicauca.bancopreguntas.tracking.application.port.HistorialRepository;
import co.unicauca.bancopreguntas.tracking.application.port.ProcessedEvents;
import co.unicauca.bancopreguntas.tracking.domain.AnalisisDeCompetencias;
import co.unicauca.bancopreguntas.tracking.domain.EstadisticasIndividuales;
import co.unicauca.bancopreguntas.tracking.domain.Pagina;
import co.unicauca.bancopreguntas.tracking.domain.ReporteDeGrupo;
import co.unicauca.bancopreguntas.tracking.domain.ResultadoRegistrado;
import co.unicauca.bancopreguntas.tracking.domain.Umbrales;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Casos de uso del seguimiento académico (RF-25 a RF-28): guarda el resultado de cada simulacro terminado y
 * responde el historial, las estadísticas, las fortalezas y debilidades de un estudiante y el reporte del grupo.
 * El manejador del evento es idempotente.
 */
@Transactional
public class TrackingApplicationService {

    private final HistorialRepository historial;
    private final ProcessedEvents procesados;
    private final Umbrales umbrales;
    private final Clock reloj;

    public TrackingApplicationService(HistorialRepository historial, ProcessedEvents procesados, Umbrales umbrales,
                                      Clock reloj) {
        this.historial = historial;
        this.procesados = procesados;
        this.umbrales = umbrales;
        this.reloj = reloj;
    }

    /** {@code simulation.completed}: agrega el resultado al historial del estudiante (RF-25). */
    public void alCompletarseUnSimulacro(String eventId, ResultadoRegistrado resultado) {
        if (!procesados.registrarSiEsNuevo(eventId)) {
            return;
        }
        historial.guardar(resultado);
    }

    @Transactional(readOnly = true)
    public Pagina<ResultadoRegistrado> historial(String estudianteId, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1) {
            throw new IllegalArgumentException("La página y el tamaño deben ser al menos 1");
        }
        return historial.delEstudiante(estudianteId, pagina, tamano);
    }

    /** Estadísticas individuales (RF-26). Un estudiante sin simulacros tiene estadísticas en cero. */
    @Transactional(readOnly = true)
    public EstadisticasIndividuales estadisticas(String estudianteId) {
        return EstadisticasIndividuales.de(estudianteId, historial.todosDelEstudiante(estudianteId));
    }

    /** Fortalezas y debilidades por competencia (RF-27). */
    @Transactional(readOnly = true)
    public AnalisisDeCompetencias competencias(String estudianteId) {
        return AnalisisDeCompetencias.de(estudianteId, historial.todosDelEstudiante(estudianteId), umbrales);
    }

    /** Reporte para docentes y coordinadores (RF-28). */
    @Transactional(readOnly = true)
    public ReporteDeGrupo reporteDeGrupo() {
        return ReporteDeGrupo.de(historial.todos(), umbrales, reloj.instant());
    }

    public Umbrales umbrales() {
        return umbrales;
    }
}
