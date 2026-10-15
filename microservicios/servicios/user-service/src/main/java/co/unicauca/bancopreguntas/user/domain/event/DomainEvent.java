package co.unicauca.bancopreguntas.user.domain.event;

/** Algo de seguridad que ocurrió y que interesa a otros servicios. {@link #type()} es también la routing key. */
public sealed interface DomainEvent permits LoginSucceeded, LoginFailed {

    String type();
}
