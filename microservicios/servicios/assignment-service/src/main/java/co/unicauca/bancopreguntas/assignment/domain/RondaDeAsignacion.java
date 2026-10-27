package co.unicauca.bancopreguntas.assignment.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Una vuelta de revisión de una versión de la pregunta, vista desde la asignación: qué revisores tiene y en qué estado
 * está la ronda. Las evaluaciones no se guardan aquí (son de evaluation-service); solo se recuerda la decisión de cada
 * revisor, para su bandeja. Pertenece al {@link CasoDeAsignacion}, que es quien la modifica; por eso sus operaciones
 * son internas al paquete.
 */
public class RondaDeAsignacion {

    private final int numero;
    private final int versionDePregunta;
    private final Instant iniciadaEn;
    private EstadoRonda estado;
    private Instant decididaEn;
    private Instant cerradaEn;
    private final Map<String, Asignacion> asignaciones = new LinkedHashMap<>();
    private final Map<String, Decision> decisiones = new LinkedHashMap<>();

    private RondaDeAsignacion(int numero, int versionDePregunta, Instant iniciadaEn, EstadoRonda estado) {
        if (numero < 1) {
            throw new IllegalArgumentException("El número de ronda debe ser al menos 1");
        }
        if (versionDePregunta < 1) {
            throw new IllegalArgumentException("La versión de la pregunta debe ser al menos 1");
        }
        this.numero = numero;
        this.versionDePregunta = versionDePregunta;
        this.iniciadaEn = iniciadaEn;
        this.estado = estado;
    }

    static RondaDeAsignacion nueva(int numero, int versionDePregunta, Instant ahora) {
        return new RondaDeAsignacion(numero, versionDePregunta, ahora, EstadoRonda.PENDIENTE_ASIGNACION);
    }

    /** Reconstruye una ronda guardada (uso de la persistencia). */
    public static RondaDeAsignacion restaurar(int numero, int versionDePregunta, EstadoRonda estado, Instant iniciadaEn,
                                              Instant decididaEn, Instant cerradaEn, List<Asignacion> asignaciones,
                                              Map<String, Decision> decisiones) {
        RondaDeAsignacion ronda = new RondaDeAsignacion(numero, versionDePregunta, iniciadaEn, estado);
        ronda.decididaEn = decididaEn;
        ronda.cerradaEn = cerradaEn;
        asignaciones.forEach(a -> ronda.asignaciones.put(a.revisorId(), a));
        ronda.decisiones.putAll(decisiones);
        return ronda;
    }

    public int numero() {
        return numero;
    }

    /** La versión de la pregunta que se revisa en esta ronda. */
    public int versionDePregunta() {
        return versionDePregunta;
    }

    public EstadoRonda estado() {
        return estado;
    }

    public Instant iniciadaEn() {
        return iniciadaEn;
    }

    public Optional<Instant> decididaEn() {
        return Optional.ofNullable(decididaEn);
    }

    public Optional<Instant> cerradaEn() {
        return Optional.ofNullable(cerradaEn);
    }

    public List<Asignacion> asignaciones() {
        return List.copyOf(asignaciones.values());
    }

    public List<String> revisores() {
        return List.copyOf(asignaciones.keySet());
    }

    public boolean tieneRevisor(String revisorId) {
        return asignaciones.containsKey(revisorId);
    }

    public int cantidadDeRevisores() {
        return asignaciones.size();
    }

    /** La decisión que el revisor informó en su evaluación, si ya evaluó. */
    public Optional<Decision> decisionDe(String revisorId) {
        return Optional.ofNullable(decisiones.get(revisorId));
    }

    public Map<String, Decision> decisiones() {
        return Map.copyOf(decisiones);
    }

    // --- operaciones internas, solo para el agregado ---

    void agregarAsignacion(Asignacion asignacion) {
        asignaciones.put(asignacion.revisorId(), asignacion);
    }

    void pasarAEnCurso() {
        estado = EstadoRonda.EN_CURSO;
    }

    void registrarDecision(String revisorId, Decision decision) {
        decisiones.putIfAbsent(revisorId, decision);
    }

    void rechazar(Instant ahora) {
        estado = EstadoRonda.RECHAZADA;
        decididaEn = ahora;
    }

    void aprobar(Instant ahora) {
        estado = EstadoRonda.APROBADA;
        decididaEn = ahora;
        cerradaEn = ahora;
    }

    void cerrar(Instant ahora) {
        estado = EstadoRonda.CERRADA;
        cerradaEn = ahora;
    }
}
