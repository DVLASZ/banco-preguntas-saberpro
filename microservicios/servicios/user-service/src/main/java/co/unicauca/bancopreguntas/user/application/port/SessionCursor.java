package co.unicauca.bancopreguntas.user.application.port;

import java.time.Instant;
import java.util.Optional;

/**
 * Puerto de salida: hasta qué momento ya se publicaron los inicios de sesión. Además sirve de cerrojo: con
 * varias réplicas del servicio, solo una a la vez lee y publica, de modo que ningún inicio se publica dos veces.
 */
public interface SessionCursor {

    /**
     * Toma el cursor y lo bloquea hasta el fin de la transacción.
     *
     * @return el momento del último inicio de sesión ya publicado, o vacío si otra réplica lo tiene tomado
     */
    Optional<Instant> tomar();

    /** Adelanta el cursor; debe llamarse después de {@link #tomar()} en la misma transacción. */
    void avanzarA(Instant momento);
}
