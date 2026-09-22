package co.unicauca.saberpro.revision.domain;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio de la HU-04: el Administrador asigna uno o más revisores a las
 * preguntas en estado Pendiente de revisión.
 *
 * <p>TODO(HU-04): implementar cada método siguiendo el contrato de su Javadoc.
 * Los cambios de estado de la pregunta se hacen con
 * {@link QuestionService#cambiarEstado}, que ya valida las transiciones (RF-15).
 */
public class AsignacionRevisionService {

    private final QuestionService questionService;
    private final DirectorioRevisores directorio;
    private final AsignacionRevisionRepository repository;
    private final NotificadorAsignacion notificador;

    public AsignacionRevisionService(QuestionService questionService, DirectorioRevisores directorio,
                                     AsignacionRevisionRepository repository, NotificadorAsignacion notificador) {
        this.questionService = questionService;
        this.directorio = directorio;
        this.repository = repository;
        this.notificador = notificador;
    }

    /**
     * Preguntas que esperan revisor (criterio 1 de la HU): las que están en
     * estado {@code PENDIENTE_REVISION}. La pantalla muestra de cada una su autor
     * ({@link Question#getAutor()}).
     */
    public List<Question> preguntasPendientes() {
        List<Question> resultado = new ArrayList<>();
        for (Question pregunta : questionService.listarPreguntas()) {
            if (pregunta.getEstado() == EstadoPregunta.PENDIENTE_REVISION) {
                resultado.add(pregunta);
            }
        }
        return resultado;
    }

    /**
     * Revisores que se pueden elegir para una pregunta: usuarios activos con
     * rol Revisor, sin incluir al autor de la pregunta (criterio 4: el autor
     * aparece deshabilitado; aquí basta con no devolverlo, o devolverlo aparte
     * si la pantalla lo quiere mostrar deshabilitado).
     */
    public List<Revisor> revisoresDisponibles(String idPregunta) {
        Question pregunta = questionService.obtenerPregunta(idPregunta);
        List<Revisor> disponibles = new ArrayList<>();
        for (Revisor revisor : directorio.revisoresActivos()) {
            if (!revisor.usuario().equals(pregunta.getAutor())) {
                disponibles.add(revisor);
            }
        }
        return disponibles;
    }

    /**
     * Asigna revisores a una pregunta (criterio 2 y 3 de la HU).
     *
     * <p>Reglas: debe haber al menos un revisor (si no, lanzar
     * {@link IllegalArgumentException} con el mensaje exacto
     * "Debe seleccionar al menos un revisor"); ningún revisor puede ser el autor;
     * la pregunta debe estar Pendiente de revisión. Si todo es válido: guarda una
     * {@link AsignacionRevision} por revisor, pasa la pregunta a {@code EN_REVISION}
     * y notifica a cada revisor con el {@link NotificadorAsignacion}.
     */
    public void asignarRevisores(String idPregunta, List<String> usuariosRevisores, String administrador) {
        if (usuariosRevisores == null || usuariosRevisores.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un revisor");
        }
        Question pregunta = questionService.obtenerPregunta(idPregunta);
        if (pregunta.getEstado() != EstadoPregunta.PENDIENTE_REVISION) {
            throw new IllegalStateException("La pregunta " + idPregunta + " no está pendiente de revisión");
        }
        for (String usuario : usuariosRevisores) {
            if (usuario.equals(pregunta.getAutor())) {
                throw new IllegalArgumentException("El autor de la pregunta no puede ser su revisor");
            }
        }
        List<Revisor> activos = directorio.revisoresActivos();
        for (String usuario : usuariosRevisores) {
            boolean esRevisorActivo = false;
            for (Revisor revisor : activos) {
                if (revisor.usuario().equals(usuario)) {
                    esRevisorActivo = true;
                    break;
                }
            }
            if (!esRevisorActivo) {
                throw new IllegalArgumentException("El usuario " + usuario + " no es un revisor activo");
            }
        }

        for (String usuario : usuariosRevisores) {
            repository.guardar(new AsignacionRevision(idPregunta, usuario, administrador, LocalDateTime.now()));
        }
        questionService.cambiarEstado(idPregunta, EstadoPregunta.EN_REVISION);
        for (String usuario : usuariosRevisores) {
            for (Revisor revisor : activos) {
                if (revisor.usuario().equals(usuario)) {
                    notificador.notificar(revisor, pregunta);
                    break;
                }
            }
        }
    }

    /** Preguntas que tiene asignadas un revisor y que siguen En revisión (para su ventana de revisión). */
    public List<Question> preguntasAsignadas(String usuarioRevisor) {
        Set<String> idsVistos = new LinkedHashSet<>();
        List<Question> resultado = new ArrayList<>();
        for (AsignacionRevision asignacion : repository.obtenerPorRevisor(usuarioRevisor)) {
            Question pregunta = questionService.obtenerPregunta(asignacion.getIdPregunta());
            if (pregunta.getEstado() == EstadoPregunta.EN_REVISION && idsVistos.add(pregunta.getId())) {
                resultado.add(pregunta);
            }
        }
        return resultado;
    }
}
