package co.unicauca.saberpro.preguntas.simulacro.domain;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.Question;

import java.util.Collections;
import java.util.List;

/**
 * Un simulacro generado por un Docente (HU-12, RF-21/RF-22): una selección
 * fija de preguntas ya {@code PUBLICADA}s, tomada en el momento en que se
 * genera, para un grupo y con un tiempo máximo de presentación (HU-13,
 * RF-23). Las preguntas quedan "congeladas" en el simulacro: si después
 * cambian de estado o contenido, no afecta a los simulacros ya generados.
 */
public class Simulacro {

    private final String id;
    private final String grupo;
    private final List<Question> preguntas;
    private final int tiempoMaximoMinutos;
    private final Competencia filtroCompetencia;
    private final String filtroTema;
    private final Dificultad filtroDificultad;

    public Simulacro(String id, String grupo, List<Question> preguntas, int tiempoMaximoMinutos,
                      Competencia filtroCompetencia, String filtroTema, Dificultad filtroDificultad) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del simulacro es obligatorio");
        }
        if (grupo == null || grupo.isBlank()) {
            throw new IllegalArgumentException("El grupo del simulacro es obligatorio");
        }
        if (preguntas == null || preguntas.isEmpty()) {
            throw new IllegalArgumentException("Un simulacro necesita al menos una pregunta");
        }
        if (tiempoMaximoMinutos <= 0) {
            throw new IllegalArgumentException("El tiempo máximo debe ser mayor a cero minutos");
        }
        this.id = id;
        this.grupo = grupo;
        this.preguntas = List.copyOf(preguntas);
        this.tiempoMaximoMinutos = tiempoMaximoMinutos;
        this.filtroCompetencia = filtroCompetencia;
        this.filtroTema = filtroTema;
        this.filtroDificultad = filtroDificultad;
    }

    public String getId() {
        return id;
    }

    public String getGrupo() {
        return grupo;
    }

    /** Preguntas del simulacro, en orden fijo de presentación. Lista inmutable. */
    public List<Question> getPreguntas() {
        return Collections.unmodifiableList(preguntas);
    }

    public int getTiempoMaximoMinutos() {
        return tiempoMaximoMinutos;
    }

    public Competencia getFiltroCompetencia() {
        return filtroCompetencia;
    }

    public String getFiltroTema() {
        return filtroTema;
    }

    public Dificultad getFiltroDificultad() {
        return filtroDificultad;
    }

    /** Descripción corta de los filtros usados, para mostrar en la lista de simulacros generados. */
    public String describirFiltros() {
        StringBuilder sb = new StringBuilder();
        sb.append(filtroCompetencia == null ? "Todas las competencias" : filtroCompetencia.toString());
        if (filtroTema != null && !filtroTema.isBlank()) {
            sb.append(" · ").append(filtroTema);
        }
        if (filtroDificultad != null) {
            sb.append(" · ").append(filtroDificultad);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return id + " " + grupo + " - " + describirFiltros() + " (" + preguntas.size() + " preguntas)";
    }
}
