package co.unicauca.bancopreguntas.review.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Una vuelta completa de revisión de una versión de la pregunta: los revisores asignados y las
 * evaluaciones que han enviado. Pertenece al {@link CasoDeRevision}, que es quien la modifica; por eso sus
 * operaciones son internas al paquete.
 */
public class RondaDeRevision {

    private final int numero;
    private final int versionDePregunta;
    private final Instant iniciadaEn;
    private EstadoRonda estado;
    private Instant decididaEn;
    private Instant cerradaEn;
    private final Map<String, Asignacion> asignaciones = new LinkedHashMap<>();
    private final Map<String, Evaluacion> evaluaciones = new LinkedHashMap<>();

    private RondaDeRevision(int numero, int versionDePregunta, Instant iniciadaEn, EstadoRonda estado) {
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

    static RondaDeRevision nueva(int numero, int versionDePregunta, Instant ahora) {
        return new RondaDeRevision(numero, versionDePregunta, ahora, EstadoRonda.PENDIENTE_ASIGNACION);
    }

    /** Reconstruye una ronda guardada (uso de la persistencia). */
    public static RondaDeRevision restaurar(int numero, int versionDePregunta, EstadoRonda estado, Instant iniciadaEn,
                                            Instant decididaEn, Instant cerradaEn,
                                            List<Asignacion> asignaciones, List<Evaluacion> evaluaciones) {
        RondaDeRevision ronda = new RondaDeRevision(numero, versionDePregunta, iniciadaEn, estado);
        ronda.decididaEn = decididaEn;
        ronda.cerradaEn = cerradaEn;
        asignaciones.forEach(a -> ronda.asignaciones.put(a.revisorId(), a));
        evaluaciones.forEach(e -> ronda.evaluaciones.put(e.revisorId(), e));
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

    public Collection<Evaluacion> evaluaciones() {
        return List.copyOf(evaluaciones.values());
    }

    public boolean tieneRevisor(String revisorId) {
        return asignaciones.containsKey(revisorId);
    }

    public Optional<Evaluacion> evaluacionDe(String revisorId) {
        return Optional.ofNullable(evaluaciones.get(revisorId));
    }

    public int cantidadDeRevisores() {
        return asignaciones.size();
    }

    public int cantidadDeEvaluaciones() {
        return evaluaciones.size();
    }

    // --- operaciones internas, solo para el agregado ---

    void agregarAsignacion(Asignacion asignacion) {
        asignaciones.put(asignacion.revisorId(), asignacion);
    }

    void pasarAEnCurso() {
        estado = EstadoRonda.EN_CURSO;
    }

    void registrar(Evaluacion evaluacion) {
        evaluaciones.put(evaluacion.revisorId(), evaluacion);
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
