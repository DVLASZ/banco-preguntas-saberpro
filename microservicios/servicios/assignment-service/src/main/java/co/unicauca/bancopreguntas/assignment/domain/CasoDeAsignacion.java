package co.unicauca.bancopreguntas.assignment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * La asignación de revisores de una pregunta a lo largo de todas sus versiones: una ronda por cada vez que el autor la
 * envía. Es la raíz del agregado y la dueña de las reglas de la asignación (HU-04):
 *
 * <ul>
 *   <li>Cada ronda tiene de 1 a N revisores (N es configurable) y el autor nunca es uno de ellos.</li>
 *   <li>En una ronda nueva se reasignan los <b>mismos</b> revisores de la anterior; el administrador puede
 *       agregar más, no quitar.</li>
 *   <li>El resultado de la ronda y las decisiones de los revisores no las toma este servicio: las informa
 *       evaluation-service con eventos, y aquí solo se recuerdan para la bandeja y para saber cuándo una ronda ya no
 *       admite revisores.</li>
 * </ul>
 */
public class CasoDeAsignacion {

    private final String id;
    private final String preguntaId;
    private final String autorId;
    private final String titulo;
    private final String competencia;
    private final Instant creadoEn;
    private Instant actualizadoEn;
    private final List<RondaDeAsignacion> rondas = new ArrayList<>();

    private CasoDeAsignacion(String id, String preguntaId, String autorId, String titulo, String competencia,
                             Instant creadoEn, Instant actualizadoEn) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador del caso es obligatorio");
        }
        if (preguntaId == null || preguntaId.isBlank()) {
            throw new IllegalArgumentException("La pregunta es obligatoria");
        }
        if (autorId == null || autorId.isBlank()) {
            throw new IllegalArgumentException("El autor es obligatorio");
        }
        this.id = id;
        this.preguntaId = preguntaId;
        this.autorId = autorId;
        this.titulo = titulo == null ? "" : titulo;
        this.competencia = competencia == null ? "" : competencia;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    /** Abre el caso de una pregunta que se envía a revisión por primera vez. Todavía no tiene rondas. */
    public static CasoDeAsignacion nuevo(String id, String preguntaId, String autorId, String titulo,
                                         String competencia, Instant ahora) {
        return new CasoDeAsignacion(id, preguntaId, autorId, titulo, competencia, ahora, ahora);
    }

    /** Reconstruye un caso guardado (uso de la persistencia). */
    public static CasoDeAsignacion restaurar(String id, String preguntaId, String autorId, String titulo,
                                             String competencia, Instant creadoEn, Instant actualizadoEn,
                                             List<RondaDeAsignacion> rondas) {
        CasoDeAsignacion caso = new CasoDeAsignacion(id, preguntaId, autorId, titulo, competencia, creadoEn, actualizadoEn);
        caso.rondas.addAll(rondas);
        return caso;
    }

    public String id() {
        return id;
    }

    public String preguntaId() {
        return preguntaId;
    }

    public String autorId() {
        return autorId;
    }

    public String titulo() {
        return titulo;
    }

    public String competencia() {
        return competencia;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    public Instant actualizadoEn() {
        return actualizadoEn;
    }

    public List<RondaDeAsignacion> rondas() {
        return Collections.unmodifiableList(rondas);
    }

    public Optional<RondaDeAsignacion> ultimaRonda() {
        return rondas.isEmpty() ? Optional.empty() : Optional.of(rondas.getLast());
    }

    /** La ronda vigente (la última). */
    public RondaDeAsignacion rondaActual() {
        return ultimaRonda().orElseThrow(() -> new OperacionNoPermitidaException(
                "La pregunta " + preguntaId + " todavía no tiene una ronda de revisión"));
    }

    /** La ronda con ese número, si existe. */
    public Optional<RondaDeAsignacion> ronda(int numero) {
        return rondas.stream().filter(r -> r.numero() == numero).findFirst();
    }

    /**
     * Inicia la ronda de una versión de la pregunta. Si es la primera, queda pendiente de que el administrador
     * asigne revisores; si no, se reasignan solos los de la ronda anterior y la ronda arranca en curso. Si ya
     * existe una ronda para esa versión (un evento repetido), la devuelve sin cambios.
     *
     * @throws OperacionNoPermitidaException si la ronda anterior sigue abierta o la pregunta ya fue aprobada
     */
    public RondaIniciada iniciarRonda(int versionDePregunta, Instant ahora) {
        Optional<RondaDeAsignacion> existente = rondas.stream()
                .filter(r -> r.versionDePregunta() == versionDePregunta)
                .findFirst();
        if (existente.isPresent()) {
            return new RondaIniciada(existente.get(), false);
        }
        RondaDeAsignacion anterior = ultimaRonda().orElse(null);
        List<String> revisoresPrevios = List.of();
        if (anterior != null) {
            switch (anterior.estado()) {
                case RECHAZADA -> anterior.cerrar(ahora); // se reabrió y el aviso de la reapertura no llegó antes
                case CERRADA -> { /* ya estaba cerrada */ }
                case APROBADA -> throw new OperacionNoPermitidaException(
                        "La pregunta " + preguntaId + " ya fue aprobada; no admite una nueva ronda");
                default -> throw new OperacionNoPermitidaException(
                        "La ronda " + anterior.numero() + " de la pregunta " + preguntaId + " sigue abierta");
            }
            revisoresPrevios = anterior.revisores();
        }
        RondaDeAsignacion ronda = RondaDeAsignacion.nueva(rondas.size() + 1, versionDePregunta, ahora);
        for (String revisor : revisoresPrevios) {
            ronda.agregarAsignacion(new Asignacion(revisor, Asignacion.SISTEMA, ahora, true));
        }
        if (!revisoresPrevios.isEmpty()) {
            ronda.pasarAEnCurso();
        }
        rondas.add(ronda);
        actualizadoEn = ahora;
        return new RondaIniciada(ronda, true);
    }

    /**
     * Asigna revisores a la ronda vigente. Sirve tanto para la asignación inicial como para agregar más a una
     * ronda en curso; nunca se quitan. Con la primera asignación la ronda pasa a estar en curso.
     *
     * @param maximo cantidad máxima de revisores de una ronda
     * @return las asignaciones nuevas
     * @throws OperacionNoPermitidaException si la ronda ya no admite asignaciones
     * @throws RevisionInvalidaException     si la lista es vacía o repetida, incluye al autor, a alguien ya
     *                                       asignado, o supera el máximo
     */
    public List<Asignacion> asignarRevisores(List<String> revisores, String asignadoPor, int maximo, Instant ahora) {
        RondaDeAsignacion ronda = rondaActual();
        if (!ronda.estado().aceptaAsignaciones()) {
            throw new OperacionNoPermitidaException("No se pueden asignar revisores: la ronda "
                    + ronda.numero() + " está " + ronda.estado());
        }
        if (revisores == null || revisores.isEmpty()) {
            throw new RevisionInvalidaException("Debe indicarse al menos un revisor");
        }
        List<String> limpios = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (String revisor : revisores) {
            if (revisor == null || revisor.isBlank()) {
                throw new RevisionInvalidaException("El identificador de un revisor está vacío");
            }
            String id = revisor.trim();
            if (!vistos.add(id)) {
                throw new RevisionInvalidaException("El revisor " + id + " está repetido en la solicitud");
            }
            if (id.equals(autorId)) {
                throw new RevisionInvalidaException("El autor de la pregunta no puede ser su propio revisor");
            }
            if (ronda.tieneRevisor(id)) {
                throw new RevisionInvalidaException("El revisor " + id + " ya está asignado a esta ronda");
            }
            limpios.add(id);
        }
        int total = ronda.cantidadDeRevisores() + limpios.size();
        if (total > maximo) {
            throw new RevisionInvalidaException("Una pregunta admite como máximo " + maximo
                    + " revisores; ya tiene " + ronda.cantidadDeRevisores() + " y se intentan agregar " + limpios.size());
        }
        List<Asignacion> nuevas = new ArrayList<>();
        for (String id : limpios) {
            Asignacion asignacion = new Asignacion(id, asignadoPor, ahora, false);
            ronda.agregarAsignacion(asignacion);
            nuevas.add(asignacion);
        }
        if (ronda.estado() == EstadoRonda.PENDIENTE_ASIGNACION) {
            ronda.pasarAEnCurso();
        }
        actualizadoEn = ahora;
        return nuevas;
    }

    /**
     * Recuerda que un revisor ya evaluó la ronda (aviso de evaluation-service). Si la ronda o el revisor no existen
     * aquí, no hace nada.
     *
     * @return {@code true} si se registró la decisión
     */
    public boolean registrarEvaluacion(int numeroDeRonda, String revisorId, Decision decision, Instant ahora) {
        RondaDeAsignacion ronda = ronda(numeroDeRonda).orElse(null);
        if (ronda == null || !ronda.tieneRevisor(revisorId) || ronda.decisionDe(revisorId).isPresent()) {
            return false;
        }
        ronda.registrarDecision(revisorId, decision);
        actualizadoEn = ahora;
        return true;
    }

    /**
     * Recuerda que la ronda se decidió (aviso de evaluation-service): queda aprobada o rechazada y ya no admite más
     * revisores. Solo cambia una ronda en curso.
     *
     * @return {@code true} si la ronda cambió de estado
     */
    public boolean registrarDecisionDeRonda(int numeroDeRonda, boolean aprobada, Instant ahora) {
        RondaDeAsignacion ronda = ronda(numeroDeRonda).orElse(null);
        if (ronda == null || ronda.estado() != EstadoRonda.EN_CURSO) {
            return false;
        }
        if (aprobada) {
            ronda.aprobar(ahora);
        } else {
            ronda.rechazar(ahora);
        }
        actualizadoEn = ahora;
        return true;
    }

    /**
     * El autor reabrió la pregunta rechazada: la ronda que seguía abierta a observaciones se cierra para siempre.
     *
     * @return {@code true} si había una ronda rechazada que cerrar
     */
    public boolean cerrarRondaPorReapertura(Instant ahora) {
        Optional<RondaDeAsignacion> ultima = ultimaRonda();
        if (ultima.isEmpty() || ultima.get().estado() != EstadoRonda.RECHAZADA) {
            return false;
        }
        ultima.get().cerrar(ahora);
        actualizadoEn = ahora;
        return true;
    }
}
