package co.unicauca.saberpro.preguntas.domain;

import co.unicauca.saberpro.preguntas.domain.validation.QuestionValidator;
import co.unicauca.saberpro.preguntas.infra.Observer;
import co.unicauca.saberpro.preguntas.infra.Subject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Servicio de dominio para la gestión de preguntas. Es el
 * {@code ConcreteSubject} del patrón Observer: cada vez que cambia una
 * pregunta, notifica a las vistas suscritas (estadísticas y gráfica) para
 * que se refresquen.
 *
 * <p>Aplica el ciclo de vida de RF-14/RF-15: el Autor crea siempre un
 * {@code BORRADOR}, solo puede modificarlo mientras siga siéndolo (RF-06) y
 * lo envía a revisión con {@link #enviarARevision}; el resto de los cambios
 * de estado pasan por {@link #cambiarEstado}, que solo acepta las
 * transiciones válidas.
 */
public class QuestionService implements Subject {

    private final QuestionRepository repository;
    private final QuestionValidator validador;
    private final List<Observer> observadores = new ArrayList<>();

    public QuestionService(QuestionRepository repository) {
        this(repository, QuestionValidator.porDefecto());
    }

    public QuestionService(QuestionRepository repository, QuestionValidator validador) {
        this.repository = repository;
        this.validador = validador;
    }

    public List<Question> listarPreguntas() {
        return repository.obtenerTodas();
    }

    /** Las preguntas creadas por un autor (HU-03: "mis preguntas"). */
    public List<Question> listarPorAutor(String autor) {
        List<Question> resultado = new ArrayList<>();
        for (Question pregunta : repository.obtenerTodas()) {
            if (pregunta.getAutor().equals(autor)) {
                resultado.add(pregunta);
            }
        }
        return resultado;
    }

    public Question obtenerPregunta(String id) {
        Question pregunta = repository.obtenerPorId(id);
        if (pregunta == null) {
            throw new NoSuchElementException("No existe una pregunta con id " + id);
        }
        return pregunta;
    }

    /**
     * Crea una pregunta nueva a nombre de un autor, siempre en estado
     * {@code BORRADOR} (HU-01). Antes de grabarla aplica la validación
     * estructural (HU03): si falla, no se guarda nada.
     *
     * @throws co.unicauca.saberpro.preguntas.domain.validation.QuestionValidationException
     *         con todos los campos que incumplen
     */
    public Question crearBorrador(ContenidoPregunta contenido, String autor) {
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException("El autor de la pregunta es obligatorio");
        }
        validador.validarOLanzar(contenido);
        Question pregunta = construir(repository.generarNuevoId(), contenido, EstadoPregunta.BORRADOR, autor.trim());
        repository.crear(pregunta);
        notificarObservadores();
        return pregunta;
    }

    /**
     * Registra una pregunta ya construida por fuera del servicio (Taller 5:
     * los plugins de {@code QuestionMicrokernel} generan la {@link Question}
     * ellos mismos, tras pasar su propio pipeline de validación) y notifica
     * a los observadores — así una pregunta generada por un plugin también
     * aparece de inmediato en las vistas de estadísticas y gráfica.
     */
    public void registrarPreguntaGenerada(Question pregunta) {
        repository.crear(pregunta);
        notificarObservadores();
    }

    /**
     * Modifica el contenido de una pregunta sin tocar su estado. Solo lo
     * puede hacer su autor y solo mientras esté en {@code BORRADOR} (RF-06).
     * Aplica la misma validación estructural que al crearla.
     */
    public void actualizarContenido(String id, ContenidoPregunta contenido, String usuario) {
        Question actual = obtenerPregunta(id);
        exigirAutor(actual, usuario);
        if (actual.getEstado() != EstadoPregunta.BORRADOR) {
            throw new OperacionNoPermitidaException(
                    "Solo se puede modificar una pregunta en estado Borrador; esta está " + actual.getEstado());
        }
        validador.validarOLanzar(contenido);
        repository.actualizar(construir(id, contenido, actual.getEstado(), actual.getAutor()));
        notificarObservadores();
    }

    /**
     * El Autor envía su borrador a revisión (HU-02): se vuelve a aplicar la
     * validación estructural y, si la pasa, la pregunta queda en
     * {@code PENDIENTE_REVISION} para que el Administrador le asigne revisor.
     */
    public void enviarARevision(String id, String usuario) {
        Question actual = obtenerPregunta(id);
        exigirAutor(actual, usuario);
        if (actual.getEstado() != EstadoPregunta.BORRADOR) {
            throw new OperacionNoPermitidaException(
                    "Solo se puede enviar a revisión una pregunta en estado Borrador; esta está " + actual.getEstado());
        }
        validador.validarOLanzar(ContenidoPregunta.de(actual));
        cambiarEstado(id, EstadoPregunta.PENDIENTE_REVISION);
    }

    /**
     * Cambia el estado de una pregunta si la transición es válida (RF-15) y
     * notifica a los observadores (vista de estadísticas y vista gráfica).
     *
     * @throws OperacionNoPermitidaException si el estado actual no puede pasar al nuevo
     */
    public void cambiarEstado(String id, EstadoPregunta nuevoEstado) {
        Question pregunta = obtenerPregunta(id);
        if (!pregunta.getEstado().puedePasarA(nuevoEstado)) {
            throw new OperacionNoPermitidaException(
                    "No se puede pasar una pregunta de " + pregunta.getEstado() + " a " + nuevoEstado);
        }
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

    private static void exigirAutor(Question pregunta, String usuario) {
        if (usuario == null || !pregunta.getAutor().equals(usuario.trim())) {
            throw new AccesoDenegadoException("Solo el autor de la pregunta puede modificarla o enviarla a revisión");
        }
    }

    private static Question construir(String id, ContenidoPregunta c, EstadoPregunta estado, String autor) {
        return Question.builder()
                .id(id)
                .nombre(c.nombre().trim())
                .contexto(c.contexto().trim())
                .enunciado(c.enunciado().trim())
                .opciones(new QuestionDistractors(c.opcionA().trim(), c.opcionB().trim(),
                        c.opcionC().trim(), c.opcionD().trim()))
                .respuestaCorrecta(c.respuestaCorrecta().trim().charAt(0))
                .justificacion(c.justificacion().trim())
                .bibliografia(c.bibliografia().trim())
                .estado(estado)
                .competencia(c.competencia())
                .tema(c.tema().trim())
                .subtema(c.subtema().trim())
                .dificultad(c.dificultad())
                .autor(autor)
                .build();
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
