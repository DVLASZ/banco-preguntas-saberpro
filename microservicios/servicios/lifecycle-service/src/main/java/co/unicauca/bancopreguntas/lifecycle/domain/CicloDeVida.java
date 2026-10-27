package co.unicauca.bancopreguntas.lifecycle.domain;

import java.time.Instant;

/**
 * El ciclo de vida de una pregunta: en qué estado está y por cuántos cambios ha pasado. Es inmutable; cada
 * transición devuelve una copia nueva y las reglas (RF-14 y RF-15) las decide la {@link FaseDePregunta} del estado
 * actual (patrón State).
 *
 * <p>No guarda el contenido de la pregunta, que es del servicio de preguntas. La {@code secuencia} cuenta los cambios
 * de estado y viaja en {@code question.state-changed}: quien guarda una copia del estado ignora los avisos más viejos
 * que los que ya aplicó. La {@code version} es la del contenido: sube cada vez que el autor reabre la pregunta.
 */
public final class CicloDeVida {

    private final String preguntaId;
    private final String autorId;
    private final EstadoPregunta estado;
    private final int version;
    private final int secuencia;
    private final Instant actualizadoEn;

    public CicloDeVida(String preguntaId, String autorId, EstadoPregunta estado, int version, int secuencia,
                       Instant actualizadoEn) {
        if (preguntaId == null || preguntaId.isBlank()) {
            throw new IllegalArgumentException("El id de la pregunta es obligatorio");
        }
        if (autorId == null || autorId.isBlank()) {
            throw new IllegalArgumentException("El autor de la pregunta es obligatorio");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de la pregunta es obligatorio");
        }
        if (version < 1 || secuencia < 1) {
            throw new IllegalArgumentException("La versión y la secuencia deben ser al menos 1");
        }
        this.preguntaId = preguntaId;
        this.autorId = autorId.trim();
        this.estado = estado;
        this.version = version;
        this.secuencia = secuencia;
        this.actualizadoEn = actualizadoEn;
    }

    /** Una pregunta recién creada: siempre empieza en Borrador, versión 1. */
    public static CicloDeVida registrar(String preguntaId, String autorId, Instant ahora) {
        return new CicloDeVida(preguntaId, autorId, EstadoPregunta.BORRADOR, 1, 1, ahora);
    }

    public String getPreguntaId() {
        return preguntaId;
    }

    public String getAutorId() {
        return autorId;
    }

    public EstadoPregunta getEstado() {
        return estado;
    }

    public int getVersion() {
        return version;
    }

    public int getSecuencia() {
        return secuencia;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    /** La fase en la que está la pregunta: ella decide qué operaciones admite. */
    public FaseDePregunta fase() {
        return estado.fase();
    }

    /** Si el autor todavía puede modificar el contenido (solo en Borrador, RF-06). */
    public boolean esEditable() {
        return fase().editable();
    }

    /** @throws OperacionNoPermitidaException si no está en Borrador */
    public CicloDeVida enviarARevision(Instant ahora) {
        return pasarA(fase().enviarARevision(), ahora);
    }

    /** @throws OperacionNoPermitidaException si no está Pendiente de revisión */
    public CicloDeVida iniciarRevision(Instant ahora) {
        return pasarA(fase().iniciarRevision(), ahora);
    }

    /** @throws OperacionNoPermitidaException si no está En revisión */
    public CicloDeVida aprobar(Instant ahora) {
        return pasarA(fase().aprobar(), ahora);
    }

    /** @throws OperacionNoPermitidaException si no está En revisión */
    public CicloDeVida rechazar(Instant ahora) {
        return pasarA(fase().rechazar(), ahora);
    }

    /** @throws OperacionNoPermitidaException si no está Aprobada */
    public CicloDeVida publicar(Instant ahora) {
        return pasarA(fase().publicar(), ahora);
    }

    /** Se puede desde cualquier estado: nunca se elimina una pregunta (RNF-16). */
    public CicloDeVida archivar(Instant ahora) {
        return pasarA(fase().archivar(), ahora);
    }

    /**
     * Devuelve la pregunta rechazada a Borrador como una versión nueva del contenido.
     *
     * @throws OperacionNoPermitidaException si no está Rechazada
     */
    public CicloDeVida reabrir(Instant ahora) {
        return new CicloDeVida(preguntaId, autorId, fase().reabrir(), version + 1, secuencia + 1, ahora);
    }

    private CicloDeVida pasarA(EstadoPregunta destino, Instant ahora) {
        if (!fase().puedePasarA(destino)) {
            throw new OperacionNoPermitidaException("No se puede pasar una pregunta de " + estado + " a " + destino);
        }
        return new CicloDeVida(preguntaId, autorId, destino, version, secuencia + 1, ahora);
    }
}
