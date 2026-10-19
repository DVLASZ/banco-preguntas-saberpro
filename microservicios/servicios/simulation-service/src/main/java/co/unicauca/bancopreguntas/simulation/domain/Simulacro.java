package co.unicauca.bancopreguntas.simulation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Un simulacro de evaluación de un estudiante: las preguntas que se le asignaron, lo que va contestando, el tiempo
 * máximo y, al terminar, la calificación. Es el agregado de este servicio: todas las reglas del simulacro viven aquí.
 *
 * <ul>
 *   <li>Solo se contesta mientras está {@code EN_CURSO} y dentro del tiempo (RF-23).</li>
 *   <li>Se puede cambiar una respuesta antes de finalizar; solo cuenta la última.</li>
 *   <li>Al finalizar, o al vencerse el tiempo, se califica automáticamente (RF-24) y queda cerrado.</li>
 * </ul>
 */
public class Simulacro {

    private final String id;
    private final String estudianteId;
    private final CriteriosDeSimulacro criterios;
    private final List<ItemDeSimulacro> items;
    private final Instant iniciadoEn;
    private final Instant venceEn;
    private EstadoSimulacro estado;
    private Instant finalizadoEn;
    private ResultadoDeSimulacro resultado;

    /** Reconstruye un simulacro guardado. */
    public Simulacro(String id, String estudianteId, CriteriosDeSimulacro criterios, List<ItemDeSimulacro> items,
                     EstadoSimulacro estado, Instant iniciadoEn, Instant venceEn, Instant finalizadoEn,
                     ResultadoDeSimulacro resultado) {
        if (id == null || id.isBlank() || estudianteId == null || estudianteId.isBlank()) {
            throw new IllegalArgumentException("El simulacro necesita identificador y estudiante");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Un simulacro necesita al menos una pregunta");
        }
        this.id = id;
        this.estudianteId = estudianteId;
        this.criterios = criterios;
        this.items = new ArrayList<>(items);
        this.estado = estado;
        this.iniciadoEn = iniciadoEn;
        this.venceEn = venceEn;
        this.finalizadoEn = finalizadoEn;
        this.resultado = resultado;
    }

    /** Empieza un simulacro nuevo con las preguntas ya escogidas; el tiempo corre desde {@code ahora}. */
    public static Simulacro iniciar(String id, String estudianteId, CriteriosDeSimulacro criterios,
                                    List<PreguntaDeCatalogo> preguntas, Instant ahora) {
        List<ItemDeSimulacro> items = new ArrayList<>();
        for (int i = 0; i < preguntas.size(); i++) {
            items.add(new ItemDeSimulacro(i + 1, preguntas.get(i), null));
        }
        return new Simulacro(id, estudianteId, criterios, items, EstadoSimulacro.EN_CURSO, ahora,
                ahora.plus(Duration.ofMinutes(criterios.duracionMinutos())), null, null);
    }

    /**
     * Registra (o cambia) la respuesta de una pregunta.
     *
     * @throws OperacionNoPermitidaException si el simulacro ya terminó o la pregunta no es de este simulacro
     * @throws TiempoAgotadoException        si el tiempo ya se agotó
     */
    public void responder(String preguntaId, char opcion, Instant ahora) {
        if (estado != EstadoSimulacro.EN_CURSO) {
            throw new OperacionNoPermitidaException("El simulacro ya terminó (" + estado + "): no se puede contestar");
        }
        if (vencido(ahora)) {
            throw new TiempoAgotadoException();
        }
        char letra = Character.toUpperCase(opcion);
        if ("ABCD".indexOf(letra) < 0) {
            throw new IllegalArgumentException("La opción debe ser A, B, C o D");
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).pregunta().preguntaId().equals(preguntaId)) {
                items.set(i, items.get(i).respondidoCon(letra));
                return;
            }
        }
        throw new OperacionNoPermitidaException("La pregunta " + preguntaId + " no hace parte de este simulacro");
    }

    /**
     * Termina el simulacro y lo califica con lo contestado hasta el momento (o hasta que venció el tiempo).
     *
     * @return la calificación
     * @throws OperacionNoPermitidaException si ya estaba terminado
     */
    public ResultadoDeSimulacro finalizar(Instant ahora) {
        if (estado != EstadoSimulacro.EN_CURSO) {
            throw new OperacionNoPermitidaException("El simulacro ya terminó (" + estado + ")");
        }
        boolean expirado = vencido(ahora);
        estado = expirado ? EstadoSimulacro.EXPIRADO : EstadoSimulacro.FINALIZADO;
        finalizadoEn = expirado ? venceEn : ahora;
        resultado = Calificador.calificar(items);
        return resultado;
    }

    /** Si el tiempo máximo ya pasó (el instante de vencimiento todavía cuenta como dentro del tiempo). */
    public boolean vencido(Instant ahora) {
        return ahora.isAfter(venceEn);
    }

    public boolean enCurso() {
        return estado == EstadoSimulacro.EN_CURSO;
    }

    /** Segundos que le quedan; 0 si ya terminó o venció. */
    public long segundosRestantes(Instant ahora) {
        if (!enCurso()) {
            return 0;
        }
        return Math.max(0, Duration.between(ahora, venceEn).getSeconds());
    }

    /** Segundos que duró: desde el inicio hasta que terminó, o hasta {@code ahora} si sigue en curso. */
    public long segundosUsados(Instant ahora) {
        Instant fin = finalizadoEn != null ? finalizadoEn : (vencido(ahora) ? venceEn : ahora);
        return Math.max(0, Duration.between(iniciadoEn, fin).getSeconds());
    }

    public long respondidas() {
        return items.stream().filter(ItemDeSimulacro::respondida).count();
    }

    /** Las respuestas correctas solo se muestran cuando el simulacro ya terminó. */
    public boolean revelaRespuestas() {
        return estado != EstadoSimulacro.EN_CURSO;
    }

    public String getId() {
        return id;
    }

    public String getEstudianteId() {
        return estudianteId;
    }

    public CriteriosDeSimulacro getCriterios() {
        return criterios;
    }

    public List<ItemDeSimulacro> getItems() {
        return List.copyOf(items);
    }

    public EstadoSimulacro getEstado() {
        return estado;
    }

    public Instant getIniciadoEn() {
        return iniciadoEn;
    }

    public Instant getVenceEn() {
        return venceEn;
    }

    public Instant getFinalizadoEn() {
        return finalizadoEn;
    }

    public ResultadoDeSimulacro getResultado() {
        return resultado;
    }
}
