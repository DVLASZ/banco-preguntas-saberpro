package co.unicauca.bancopreguntas.question.application;

import co.unicauca.bancopreguntas.question.application.port.CicloDeVidaDePreguntas;
import co.unicauca.bancopreguntas.question.application.port.EstadoDeCiclo;
import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.OperacionNoPermitidaException;
import co.unicauca.bancopreguntas.question.domain.Question;
import co.unicauca.bancopreguntas.question.domain.QuestionNotFoundException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Doble de prueba de lifecycle-service: lleva el estado de cada pregunta con las transiciones que este servicio pide
 * (enviar, reabrir, publicar) y deja simular las decisiones de la revisión con {@link #forzar}. Puede simular que el
 * otro servicio está caído.
 */
class CicloDeVidaDePrueba implements CicloDeVidaDePreguntas {

    private record Registro(EstadoPregunta estado, int version, int secuencia) {
    }

    private final Clock reloj;
    private final Map<String, Registro> registros = new HashMap<>();
    private final Map<String, List<CambioEstado>> historiales = new HashMap<>();
    private final List<String> llamadas = new ArrayList<>();
    private boolean caido;

    CicloDeVidaDePrueba(Clock reloj) {
        this.reloj = reloj;
    }

    void caer() {
        caido = true;
    }

    List<String> llamadas() {
        return List.copyOf(llamadas);
    }

    /** Simula que la revisión (u otro servicio) dejó la pregunta en ese estado; devuelve lo que informaría el aviso. */
    EstadoDeCiclo forzar(String preguntaId, EstadoPregunta estado) {
        Registro r = registros.get(preguntaId);
        return mover(preguntaId, r, estado, r.version());
    }

    @Override
    public EstadoDeCiclo registrar(String preguntaId) {
        verificar("registrar");
        Registro r = new Registro(EstadoPregunta.BORRADOR, 1, 1);
        registros.put(preguntaId, r);
        historiales.computeIfAbsent(preguntaId, k -> new ArrayList<>())
                .add(new CambioEstado(null, EstadoPregunta.BORRADOR, "autor", reloj.instant()));
        return new EstadoDeCiclo(r.estado(), r.version(), r.secuencia());
    }

    @Override
    public EstadoDeCiclo enviarARevision(Question pregunta) {
        verificar("enviar");
        Registro r = existente(pregunta.getId());
        exigir(r, EstadoPregunta.BORRADOR, "enviar a revisión una pregunta en estado Borrador");
        return mover(pregunta.getId(), r, EstadoPregunta.PENDIENTE_REVISION, r.version());
    }

    @Override
    public EstadoDeCiclo reabrir(String preguntaId) {
        verificar("reabrir");
        Registro r = existente(preguntaId);
        exigir(r, EstadoPregunta.RECHAZADA, "reabrir una pregunta Rechazada");
        return mover(preguntaId, r, EstadoPregunta.BORRADOR, r.version() + 1);
    }

    @Override
    public EstadoDeCiclo publicar(Question pregunta) {
        verificar("publicar");
        Registro r = existente(pregunta.getId());
        exigir(r, EstadoPregunta.APROBADA, "publicar una pregunta Aprobada");
        return mover(pregunta.getId(), r, EstadoPregunta.PUBLICADA, r.version());
    }

    @Override
    public List<CambioEstado> historial(String preguntaId) {
        verificar("historial");
        return List.copyOf(historiales.getOrDefault(preguntaId, List.of()));
    }

    private EstadoDeCiclo mover(String id, Registro r, EstadoPregunta destino, int version) {
        Registro nuevo = new Registro(destino, version, r.secuencia() + 1);
        registros.put(id, nuevo);
        historiales.computeIfAbsent(id, k -> new ArrayList<>()).add(new CambioEstado(r.estado(), destino, "usuario", reloj.instant()));
        return new EstadoDeCiclo(nuevo.estado(), nuevo.version(), nuevo.secuencia());
    }

    private Registro existente(String id) {
        Registro r = registros.get(id);
        if (r == null) {
            throw new QuestionNotFoundException(id);
        }
        return r;
    }

    private static void exigir(Registro r, EstadoPregunta esperado, String queSePuede) {
        if (r.estado() != esperado) {
            throw new OperacionNoPermitidaException("Solo se puede " + queSePuede + "; esta está " + r.estado());
        }
    }

    private void verificar(String operacion) {
        llamadas.add(operacion);
        if (caido) {
            throw new DependenciaNoDisponibleException("El servicio de ciclo de vida no está disponible", null);
        }
    }
}
