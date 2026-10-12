package co.unicauca.bancopreguntas.review.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * La revisión de una pregunta a lo largo de todas sus versiones: una ronda por cada vez que el autor la envía.
 * Es la raíz del agregado y la dueña de las reglas de la revisión:
 *
 * <ul>
 *   <li>Cada ronda tiene de 1 a N revisores (N es configurable) y el autor nunca es uno de ellos.</li>
 *   <li>En una ronda nueva se reasignan los <b>mismos</b> revisores de la anterior; el administrador puede
 *       agregar más, no quitar.</li>
 *   <li>La decisión la toma una {@link PoliticaDeDecision} (unanimidad); un rechazo devuelve la pregunta al
 *       autor sin esperar a los demás.</li>
 *   <li>Tras el rechazo la ronda sigue abierta: los revisores que faltaban pueden dejar su observación, que
 *       no cambia el resultado, hasta que el autor reabre la pregunta.</li>
 *   <li>Una evaluación enviada es definitiva; las observaciones son obligatorias al rechazar.</li>
 * </ul>
 */
public class CasoDeRevision {

    /** Longitud máxima de una observación. */
    public static final int MAXIMO_DE_OBSERVACION = 4000;

    private final String id;
    private final String preguntaId;
    private final String autorId;
    private final String titulo;
    private final String competencia;
    private final Instant creadoEn;
    private Instant actualizadoEn;
    private final List<RondaDeRevision> rondas = new ArrayList<>();

    private CasoDeRevision(String id, String preguntaId, String autorId, String titulo, String competencia,
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
    public static CasoDeRevision nuevo(String id, String preguntaId, String autorId, String titulo,
                                       String competencia, Instant ahora) {
        return new CasoDeRevision(id, preguntaId, autorId, titulo, competencia, ahora, ahora);
    }

    /** Reconstruye un caso guardado (uso de la persistencia). */
    public static CasoDeRevision restaurar(String id, String preguntaId, String autorId, String titulo,
                                           String competencia, Instant creadoEn, Instant actualizadoEn,
                                           List<RondaDeRevision> rondas) {
        CasoDeRevision caso = new CasoDeRevision(id, preguntaId, autorId, titulo, competencia, creadoEn, actualizadoEn);
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

    public List<RondaDeRevision> rondas() {
        return Collections.unmodifiableList(rondas);
    }

    public Optional<RondaDeRevision> ultimaRonda() {
        return rondas.isEmpty() ? Optional.empty() : Optional.of(rondas.getLast());
    }

    /** La ronda vigente (la última). */
    public RondaDeRevision rondaActual() {
        return ultimaRonda().orElseThrow(() -> new OperacionNoPermitidaException(
                "La pregunta " + preguntaId + " todavía no tiene una ronda de revisión"));
    }

    /**
     * Inicia la ronda de una versión de la pregunta. Si es la primera, queda pendiente de que el administrador
     * asigne revisores; si no, se reasignan solos los de la ronda anterior y la ronda arranca en curso. Si ya
     * existe una ronda para esa versión (un evento repetido), la devuelve sin cambios.
     *
     * @throws OperacionNoPermitidaException si la ronda anterior sigue abierta o la pregunta ya fue aprobada
     */
    public RondaIniciada iniciarRonda(int versionDePregunta, Instant ahora) {
        Optional<RondaDeRevision> existente = rondas.stream()
                .filter(r -> r.versionDePregunta() == versionDePregunta)
                .findFirst();
        if (existente.isPresent()) {
            return new RondaIniciada(existente.get(), false);
        }
        RondaDeRevision anterior = ultimaRonda().orElse(null);
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
        RondaDeRevision ronda = RondaDeRevision.nueva(rondas.size() + 1, versionDePregunta, ahora);
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
        RondaDeRevision ronda = rondaActual();
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
        RondaDeRevision ronda = rondaActual();
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
        Optional<RondaDeRevision> ultima = ultimaRonda();
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

    private static VistaDelCaso.VistaDeRonda vistaDe(RondaDeRevision ronda, boolean detalleVisible) {
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
