package co.unicauca.saberpro.preguntas.domain;

import co.unicauca.saberpro.preguntas.infra.Observer;
import co.unicauca.saberpro.preguntas.infra.Subject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Servicio de dominio para la gestión de preguntas. Es el
 * {@code ConcreteSubject} del patrón Observer: cada vez que el estado de
 * una pregunta cambia, notifica a las vistas suscritas (estadísticas y
 * gráfica) para que se refresquen.
 */
public class QuestionService implements Subject {

    private final QuestionRepository repository;
    private final List<Observer> observadores = new ArrayList<>();

    public QuestionService(QuestionRepository repository) {
        this.repository = repository;
    }

    public List<Question> listarPreguntas() {
        return repository.obtenerTodas();
    }

    public Question obtenerPregunta(String id) {
        Question pregunta = repository.obtenerPorId(id);
        if (pregunta == null) {
            throw new NoSuchElementException("No existe una pregunta con id " + id);
        }
        return pregunta;
    }

    /**
     * Crea una pregunta nueva a nombre del Autor (HU-03, HU-04). Queda
     * directamente en {@code PENDIENTE_REVISION}: el Autor redacta y
     * envía, pero no decide su aprobación — esa es competencia exclusiva
     * del Revisor (ver {@code cambiarEstado}, usado desde
     * {@code GUIRevisor}).
     */
    public Question crearPregunta(String nombre, String enunciado, QuestionDistractors opciones,
                                   char respuestaCorrecta, Competencia competencia, String tema,
                                   Dificultad dificultad) {
        String id = repository.generarNuevoId();
        Question pregunta = new Question(id, nombre, enunciado, opciones, respuestaCorrecta,
                EstadoPregunta.PENDIENTE_REVISION, competencia, tema, dificultad);
        repository.crear(pregunta);
        notificarObservadores();
        return pregunta;
    }

    /**
     * Actualiza el contenido (nombre, enunciado, opciones, respuesta,
     * competencia, tema, dificultad) de una pregunta existente sin tocar
     * su estado. Es la operación propia del Autor al corregir una de sus
     * preguntas — el estado solo lo cambia el Revisor.
     */
    public void actualizarContenido(String id, String nombre, String enunciado, QuestionDistractors opciones,
                                     char respuestaCorrecta, Competencia competencia, String tema,
                                     Dificultad dificultad) {
        Question actual = obtenerPregunta(id);
        Question actualizada = new Question(id, nombre, enunciado, opciones, respuestaCorrecta,
                actual.getEstado(), competencia, tema, dificultad);
        repository.actualizar(actualizada);
        notificarObservadores();
    }

    /**
     * Cambia el estado de una pregunta y notifica a los observadores
     * (vista de estadísticas y vista gráfica) del cambio.
     */
    public void cambiarEstado(String id, EstadoPregunta nuevoEstado) {
        Question pregunta = obtenerPregunta(id);
        pregunta.setEstado(nuevoEstado);
        repository.actualizar(pregunta);
        notificarObservadores();
    }

    /** Cuenta cuántas preguntas hay actualmente en cada estado. */
    public Map<EstadoPregunta, Long> contarPorEstado() {
        Map<EstadoPregunta, Long> conteo = new EnumMap<>(EstadoPregunta.class);
        for (EstadoPregunta estado : EstadoPregunta.values()) {
            conteo.put(estado, 0L);
        }
        for (Question pregunta : repository.obtenerTodas()) {
            conteo.merge(pregunta.getEstado(), 1L, Long::sum);
        }
        return conteo;
    }

    /**
     * Busca preguntas ya {@code PUBLICADA}s que coincidan con los filtros
     * dados (HU-05, HU-12): solo las publicadas pueden usarse para
     * consultas de otros roles o para armar un simulacro (RF-21, RF-22).
     * Cualquier filtro en {@code null} (o, en el caso de {@code tema},
     * vacío) se ignora.
     */
    public List<Question> buscarPublicadas(Competencia competencia, String tema, Dificultad dificultad) {
        List<Question> resultado = new ArrayList<>();
        String temaBuscado = tema == null ? null : tema.trim().toLowerCase();
        for (Question pregunta : repository.obtenerTodas()) {
            if (pregunta.getEstado() != EstadoPregunta.PUBLICADA) {
                continue;
            }
            if (competencia != null && pregunta.getCompetencia() != competencia) {
                continue;
            }
            if (temaBuscado != null && !temaBuscado.isEmpty()
                    && !pregunta.getTema().toLowerCase().contains(temaBuscado)) {
                continue;
            }
            if (dificultad != null && pregunta.getDificultad() != dificultad) {
                continue;
            }
            resultado.add(pregunta);
        }
        return resultado;
    }

    @Override
    public void agregarObservador(Observer observador) {
        observadores.add(observador);
    }

    @Override
    public void eliminarObservador(Observer observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificarObservadores() {
        for (Observer observador : observadores) {
            observador.actualizar();
        }
    }
}
