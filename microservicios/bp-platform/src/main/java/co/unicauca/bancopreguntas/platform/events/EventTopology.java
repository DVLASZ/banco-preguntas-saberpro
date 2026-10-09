package co.unicauca.bancopreguntas.platform.events;

/** Nombres compartidos de la mensajería: todos los servicios publican en el mismo exchange de eventos. */
public final class EventTopology {

    /** Exchange de tipo topic; la routing key de cada mensaje es el tipo del evento. */
    public static final String EXCHANGE = "bancopreguntas.events";

    private EventTopology() {
    }
}
