package co.unicauca.bancopreguntas.evaluation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * La evaluación de una pregunta a lo largo de todas sus versiones: una ronda por cada vez que el autor la envía.
 * Es la raíz del agregado y la dueña de las reglas de la evaluación (HU-05). Los revisores de cada ronda los asigna
 * assignment-service y llegan por evento; aquí solo se evalúa:
 *
 * <ul>
 *   <li>La decisión la toma una {@link PoliticaDeDecision} (unanimidad); un rechazo devuelve la pregunta al
 *       autor sin esperar a los demás.</li>
 *   <li>Tras el rechazo la ronda sigue abierta: los revisores que faltaban pueden dejar su observación, que
 *       no cambia el resultado, hasta que el autor reabre la pregunta.</li>
 *   <li>Una evaluación enviada es definitiva; las observaciones son obligatorias al rechazar.</li>
 * </ul>
 */
public class CasoDeEvaluacion {

    /** Longitud máxima de una observación. */
    public static final int MAXIMO_DE_OBSERVACION = 4000;

    private final String id;
    private final String preguntaId;
    private final String autorId;
    private final String titulo;
    private final String competencia;
    private final Instant creadoEn;
    private Instant actualizadoEn;
    private final List<RondaDeEvaluacion> rondas = new ArrayList<>();

    private CasoDeEvaluacion(String id, String preguntaId, String autorId, String titulo, String competencia,
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
    public static CasoDeEvaluacion nuevo(String id, String preguntaId, String autorId, String titulo,
                                       String competencia, Instant ahora) {
        return new CasoDeEvaluacion(id, preguntaId, autorId, titulo, competencia, ahora, ahora);
    }

    /** Reconstruye un caso guardado (uso de la persistencia). */
    public static CasoDeEvaluacion restaurar(String id, String preguntaId, String autorId, String titulo,
                                           String competencia, Instant creadoEn, Instant actualizadoEn,
                                           List<RondaDeEvaluacion> rondas) {
        CasoDeEvaluacion caso = new CasoDeEvaluacion(id, preguntaId, autorId, titulo, competencia, creadoEn, actualizadoEn);
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

    public List<RondaDeEvaluacion> rondas() {
        return Collections.unmodifiableList(rondas);
    }

    public Optional<RondaDeEvaluacion> ultimaRonda() {
        return rondas.isEmpty() ? Optional.empty() : Optional.of(rondas.getLast());
    }

    /** La ronda vigente (la última). */
    public RondaDeEvaluacion rondaActual() {
        return ultimaRonda().orElseThrow(() -> new OperacionNoPermitidaException(
                "La pregunta " + preguntaId + " todavía no tiene una ronda de revisión"));
    }

    /**
     * Inicia la ronda de una versión de la pregunta. Queda pendiente de que lleguen los revisores asignados. Si ya
     * existe una ronda para esa versión (un evento repetido), la devuelve sin cambios.
     *
     * @throws OperacionNoPermitidaException si la ronda anterior sigue abierta o la pregunta ya fue aprobada
     */
    public RondaIniciada iniciarRonda(int versionDePregunta, Instant ahora) {
        Optional<RondaDeEvaluacion> existente = rondas.stream()
                .filter(r -> r.versionDePregunta() == versionDePregunta)
                .findFirst();
        if (existente.isPresent()) {
            return new RondaIniciada(existente.get(), false);
        }
        RondaDeEvaluacion anterior = ultimaRonda().orElse(null);
        if (anterior != null) {
            switch (anterior.estado()) {
                case RECHAZADA -> anterior.cerrar(ahora); // se reabrió y el aviso de la reapertura no llegó antes
                case CERRADA -> { /* ya estaba cerrada */ }
                case APROBADA -> throw new OperacionNoPermitidaException(
                        "La pregunta " + preguntaId + " ya fue aprobada; no admite una nueva ronda");
                default -> throw new OperacionNoPermitidaException(
                        "La ronda " + anterior.numero() + " de la pregunta " + preguntaId + " sigue abierta");
            }
        }
        RondaDeEvaluacion ronda = RondaDeEvaluacion.nueva(rondas.size() + 1, versionDePregunta, ahora);
        rondas.add(ronda);
        actualizadoEn = ahora;
        return new RondaIniciada(ronda, true);
    }

    /**
     * Registra los revisores que assignment-service asignó a una ronda (aviso {@code review.reviewers-assigned}). Los
     * que ya estaban se ignoran; con los primeros la ronda pasa a estar en curso.
     *
     * @return cuántos revisores nuevos se registraron
     * @throws OperacionNoPermitidaException si la ronda no existe o ya no admite asignaciones
     */
    public int registrarAsignaciones(int numeroDeRonda, List<String> revisores, String asignadoPor, boolean automatica,
                                     Instant ahora) {
        RondaDeEvaluacion ronda = rondas.stream().filter(r -> r.numero() == numeroDeRonda).findFirst()
                .orElseThrow(() -> new OperacionNoPermitidaException(
                        "La pregunta " + preguntaId + " no tiene la ronda " + numeroDeRonda));
        if (!ronda.estado().aceptaAsignaciones()) {
            throw new OperacionNoPermitidaException("No se pueden registrar revisores: la ronda "
                    + ronda.numero() + " está " + ronda.estado());
        }
        int nuevos = 0;
        for (String revisor : revisores) {
            if (revisor != null && !revisor.isBlank() && !ronda.tieneRevisor(revisor.trim())) {
                ronda.agregarAsignacion(new Asignacion(revisor.trim(), asignadoPor, ahora, automatica));
                nuevos++;
            }
        }
        if (nuevos > 0 && ronda.estado() == EstadoRonda.PENDIENTE_ASIGNACION) {
            ronda.pasarAEnCurso();
        }
        if (nuevos > 0) {
            actualizadoEn = ahora;
        }
        return nuevos;
    }

    /**
     * Registra la evaluación de un revisor sobre la ronda vigente y, si completa la decisión según la política,
     * decide la ronda. Si la ronda ya estaba rechazada, la evaluación se guarda como observación tardía y no
     * cambia el resultado.
     *
     * @throws RevisionInvalidaException     si falta la decisión, o la observación al rechazar, o es demasiado larga
     * @throws AccesoNoPermitidoException    si el revisor no está asignado a la ronda
     * @throws OperacionNoPermitidaException si la ronda no admite evaluaciones o el revisor ya evaluó
     */
    public ResultadoDeEvaluacion evaluar(String revisorId, Decision decision, String observacion,
                                         PoliticaDeDecision politica, Instant ahora) {
        if (decision == null) {
            throw new RevisionInvalidaException("La decisión es obligatoria");
        }
        String texto = observacion == null || observacion.isBlank() ? null : observacion.trim();
        if (decision == Decision.RECHAZAR && texto == null) {
            throw new RevisionInvalidaException("Debes escribir una observación para rechazar la pregunta");
        }
        if (texto != null && texto.length() > MAXIMO_DE_OBSERVACION) {
            throw new RevisionInvalidaException("La observación no puede superar " + MAXIMO_DE_OBSERVACION + " caracteres");
        }
        RondaDeEvaluacion ronda = rondaActual();
        if (revisorId == null || !ronda.tieneRevisor(revisorId)) {
            throw new AccesoNoPermitidoException("No estás asignado como revisor de esta pregunta");
        }
        EstadoRonda estado = ronda.estado();
        if (estado != EstadoRonda.EN_CURSO && estado != EstadoRonda.RECHAZADA) {
            throw new OperacionNoPermitidaException("La ronda " + ronda.numero() + " no admite evaluaciones: está " + estado);
        }
        if (ronda.evaluacionDe(revisorId).isPresent()) {
            throw new OperacionNoPermitidaException("Ya evaluaste esta pregunta; la evaluación es definitiva");
        }
        boolean tardia = estado == EstadoRonda.RECHAZADA;
        Evaluacion evaluacion = new Evaluacion(revisorId, decision, texto, ahora, tardia);
        ronda.registrar(evaluacion);
        actualizadoEn = ahora;
        if (tardia) {
            return new ResultadoDeEvaluacion(evaluacion, ronda, ResultadoDeDecision.PENDIENTE);
        }
        ResultadoDeDecision resultado = politica.decidir(ronda.revisores(), ronda.evaluaciones());
        switch (resultado) {
            case RECHAZADA -> ronda.rechazar(ahora);
            case APROBADA -> ronda.aprobar(ahora);
            case PENDIENTE -> { /* faltan evaluaciones */ }
        }
        return new ResultadoDeEvaluacion(evaluacion, ronda, resultado);
    }

    /**
     * El autor reabrió la pregunta rechazada: la ronda que seguía abierta a observaciones se cierra para siempre
     * y lo que no se evaluó se descarta.
     *
     * @return {@code true} si había una ronda rechazada que cerrar
     */
    public boolean cerrarRondaPorReapertura(Instant ahora) {
        Optional<RondaDeEvaluacion> ultima = ultimaRonda();
        if (ultima.isEmpty() || ultima.get().estado() != EstadoRonda.RECHAZADA) {
            return false;
        }
        ultima.get().cerrar(ahora);
        actualizadoEn = ahora;
        return true;
    }

    /** Si el usuario es o fue revisor en alguna ronda de este caso. */
    public boolean participaComoRevisor(String usuarioId) {
        return rondas.stream().anyMatch(r -> r.tieneRevisor(usuarioId));
    }

    /**
     * La revisión tal como la ve un observador.
     *
     * @throws AccesoNoPermitidoException si no es el administrador, el autor ni un revisor del caso
     */
    public VistaDelCaso vistaPara(Observador observador) {
        boolean esAutor = observador.id().equals(autorId);
        boolean esRevisor = participaComoRevisor(observador.id());
        if (!observador.administrador() && !esAutor && !esRevisor) {
            throw new AccesoNoPermitidoException("No participas en la revisión de esta pregunta");
        }
        boolean veTodo = observador.administrador() || esRevisor;
        List<VistaDelCaso.VistaDeRonda> vistas = rondas.stream()
                .map(r -> vistaDe(r, veTodo || r.estado().decidida()))
                .collect(Collectors.toList());
        return new VistaDelCaso(preguntaId, autorId, titulo, competencia, vistas);
    }

    private static VistaDelCaso.VistaDeRonda vistaDe(RondaDeEvaluacion ronda, boolean detalleVisible) {
        List<VistaDelCaso.VistaDeRevisor> revisores = detalleVisible
                ? ronda.asignaciones().stream()
                        .map(a -> new VistaDelCaso.VistaDeRevisor(a.revisorId(), a.asignadoPor(), a.asignadoEn(),
                                a.automatica(), ronda.evaluacionDe(a.revisorId()).orElse(null)))
                        .collect(Collectors.toList())
                : List.of();
        return new VistaDelCaso.VistaDeRonda(ronda.numero(), ronda.versionDePregunta(), ronda.estado(),
                ronda.iniciadaEn(), ronda.decididaEn().orElse(null), ronda.cerradaEn().orElse(null),
                ronda.cantidadDeRevisores(), ronda.cantidadDeEvaluaciones(), detalleVisible, revisores);
    }
}
