package co.unicauca.bancopreguntas.question.infrastructure.remote;

import co.unicauca.bancopreguntas.question.application.port.CicloDeVidaDePreguntas;
import co.unicauca.bancopreguntas.question.application.port.EstadoDeCiclo;
import co.unicauca.bancopreguntas.question.domain.CambioEstado;
import co.unicauca.bancopreguntas.question.domain.DependenciaNoDisponibleException;
import co.unicauca.bancopreguntas.question.domain.EstadoPregunta;
import co.unicauca.bancopreguntas.question.domain.Question;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Adaptador del puerto {@link CicloDeVidaDePreguntas}: pide las transiciones a lifecycle-service por REST (patrón
 * Adapter). El cliente viene de la configuración común, que le pasa la identidad de quien hizo la petición, para que
 * el otro servicio vuelva a exigir el rol y compruebe que solo el autor mueve su pregunta.
 */
@Component
public class CicloDeVidaRemoto implements CicloDeVidaDePreguntas {

    private static final String RUTA = "/api/lifecycle/questions";

    private final RestClient cliente;

    public CicloDeVidaRemoto(RestClient.Builder constructor, @Value("${question.lifecycle.url:http://localhost:8094}") String url) {
        this.cliente = constructor.baseUrl(url).build();
    }

    @Override
    public EstadoDeCiclo registrar(String preguntaId) {
        return transicion(preguntaId, RUTA, Map.of("preguntaId", preguntaId));
    }

    @Override
    public EstadoDeCiclo enviarARevision(Question pregunta) {
        return transicion(pregunta.getId(), RUTA + "/" + pregunta.getId() + "/submit",
                Map.of("titulo", pregunta.getNombre(), "competencia", pregunta.getCompetencia().name()));
    }

    @Override
    public EstadoDeCiclo reabrir(String preguntaId) {
        return transicion(preguntaId, RUTA + "/" + preguntaId + "/reopen", null);
    }

    @Override
    public EstadoDeCiclo publicar(Question p) {
        Map<String, String> contenido = Map.ofEntries(
                Map.entry("titulo", p.getNombre()),
                Map.entry("competencia", p.getCompetencia().name()),
                Map.entry("tema", p.getTema()),
                Map.entry("subtema", p.getSubtema()),
                Map.entry("dificultad", p.getDificultad().name()),
                Map.entry("contexto", p.getContexto()),
                Map.entry("enunciado", p.getEnunciado()),
                Map.entry("opcionA", p.getOpciones().getOpcionA()),
                Map.entry("opcionB", p.getOpciones().getOpcionB()),
                Map.entry("opcionC", p.getOpciones().getOpcionC()),
                Map.entry("opcionD", p.getOpciones().getOpcionD()),
                Map.entry("respuestaCorrecta", String.valueOf(p.getRespuestaCorrecta())));
        return transicion(p.getId(), RUTA + "/" + p.getId() + "/publish", contenido);
    }

    @Override
    public List<CambioEstado> historial(String preguntaId) {
        List<CambioRemoto> cambios = ErroresRemotos.llamar("ciclo de vida", preguntaId, () -> cliente.get()
                .uri(RUTA + "/{id}/history", preguntaId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), ErroresRemotos.rechazos(preguntaId))
                .body(new ParameterizedTypeReference<List<CambioRemoto>>() { }));
        if (cambios == null) {
            throw new DependenciaNoDisponibleException("El servicio de ciclo de vida respondió sin contenido", null);
        }
        return cambios.stream().map(c -> new CambioEstado(c.desde() == null ? null : EstadoPregunta.valueOf(c.desde()),
                EstadoPregunta.valueOf(c.hacia()), c.cambiadoPor(), c.fecha())).toList();
    }

    private EstadoDeCiclo transicion(String preguntaId, String ruta, Object cuerpo) {
        RespuestaCiclo r = ErroresRemotos.llamar("ciclo de vida", preguntaId, () -> {
            RestClient.RequestBodySpec peticion = cliente.post().uri(ruta).contentType(MediaType.APPLICATION_JSON);
            if (cuerpo != null) {
                peticion = peticion.body(cuerpo);
            }
            return peticion.retrieve()
                    .onStatus(status -> status.is4xxClientError(), ErroresRemotos.rechazos(preguntaId))
                    .body(RespuestaCiclo.class);
        });
        if (r == null) {
            throw new DependenciaNoDisponibleException("El servicio de ciclo de vida respondió sin contenido", null);
        }
        return new EstadoDeCiclo(EstadoPregunta.valueOf(r.estado()), r.version(), r.secuencia());
    }

    /** El estado que devuelve lifecycle-service. */
    record RespuestaCiclo(String estado, int version, int secuencia) {
    }

    /** Una entrada del historial tal como la devuelve lifecycle-service. */
    record CambioRemoto(String desde, String hacia, String cambiadoPor, Instant fecha) {
    }
}
