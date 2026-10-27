package co.unicauca.bancopreguntas.lifecycle.domain;

import java.util.Set;

/**
 * Patrón State: cada estado del ciclo de vida de una pregunta es una clase que sabe qué se puede hacer con una
 * pregunta que está en ese estado (RF-14 y RF-15). Las operaciones devuelven el estado al que pasa la pregunta, o
 * lanzan {@link OperacionNoPermitidaException} si ese estado no la admite; así las reglas del ciclo de vida
 * viven en un solo lugar y no en condicionales repartidos por el servicio.
 *
 * <p>{@link EstadoPregunta} sigue siendo el identificador del estado (se guarda y viaja en los eventos); esta
 * jerarquía es su comportamiento. Para agregar un estado nuevo basta una clase más.
 */
public sealed interface FaseDePregunta permits FaseBorrador, FasePendienteDeRevision, FaseEnRevision, FaseAprobada,
        FaseRechazada, FasePublicada, FaseArchivada {

    /** El estado que esta fase representa. */
    EstadoPregunta estado();

    /** A qué estados puede pasar una pregunta en esta fase, sin contar archivarla (se puede desde cualquier estado). */
    Set<EstadoPregunta> destinos();

    /** Solo en Borrador el autor puede modificar el contenido (RF-06). */
    default boolean editable() {
        return false;
    }

    /** El autor envía su borrador a revisión (HU-02). */
    default EstadoPregunta enviarARevision() {
        throw rechazo("enviar a revisión una pregunta en estado Borrador");
    }

    /** Se asignaron revisores: empieza la revisión (HU-04). */
    default EstadoPregunta iniciarRevision() {
        throw rechazo("iniciar la revisión de una pregunta Pendiente de revisión");
    }

    /** Todos los revisores aprobaron (HU-05). */
    default EstadoPregunta aprobar() {
        throw rechazo("aprobar una pregunta En revisión");
    }

    /** Un revisor rechazó (HU-05). */
    default EstadoPregunta rechazar() {
        throw rechazo("rechazar una pregunta En revisión");
    }

    /** El autor reabre una pregunta rechazada para corregirla. */
    default EstadoPregunta reabrir() {
        throw rechazo("reabrir una pregunta Rechazada");
    }

    /** El administrador publica una pregunta aprobada. */
    default EstadoPregunta publicar() {
        throw rechazo("publicar una pregunta Aprobada");
    }

    /** Nunca se elimina una pregunta (RNF-16): desde cualquier estado se puede archivar. */
    default EstadoPregunta archivar() {
        return EstadoPregunta.ARCHIVADA;
    }

    /** Si una pregunta en esta fase puede pasar al estado indicado (RF-15). */
    default boolean puedePasarA(EstadoPregunta destino) {
        return destino != null && (destino == EstadoPregunta.ARCHIVADA || destinos().contains(destino));
    }

    /** @throws OperacionNoPermitidaException si la fase no permite modificar el contenido */
    default void exigirEditable() {
        if (!editable()) {
            throw rechazo("modificar una pregunta en estado Borrador");
        }
    }

    /** La fase que corresponde a un estado. */
    static FaseDePregunta de(EstadoPregunta estado) {
        return switch (estado) {
            case BORRADOR -> new FaseBorrador();
            case PENDIENTE_REVISION -> new FasePendienteDeRevision();
            case EN_REVISION -> new FaseEnRevision();
            case APROBADA -> new FaseAprobada();
            case RECHAZADA -> new FaseRechazada();
            case PUBLICADA -> new FasePublicada();
            case ARCHIVADA -> new FaseArchivada();
        };
    }

    private OperacionNoPermitidaException rechazo(String queSePuede) {
        return new OperacionNoPermitidaException("Solo se puede " + queSePuede + "; esta está " + estado());
    }
}
