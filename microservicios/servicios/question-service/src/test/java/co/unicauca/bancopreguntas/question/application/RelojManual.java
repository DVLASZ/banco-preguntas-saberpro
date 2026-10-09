package co.unicauca.bancopreguntas.question.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj de prueba que solo avanza cuando el test lo pide. */
class RelojManual extends Clock {

    private Instant ahora;

    RelojManual(Instant inicio) {
        this.ahora = inicio;
    }

    void avanzar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
