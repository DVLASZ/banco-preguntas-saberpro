package co.unicauca.saberpro.preguntas.domain;

import java.util.List;

/** Fuente por defecto del Revisor: cualquier pregunta que esté pendiente de revisión o en revisión. */
public class TodasLasPreguntasEnRevision implements FuenteDePreguntasParaRevisar {

    private final QuestionService service;

    public TodasLasPreguntasEnRevision(QuestionService service) {
        this.service = service;
    }

    @Override
    public List<Question> paraRevisor(String usuarioRevisor) {
        return service.listarPreguntas().stream()
                .filter(p -> p.getEstado() == EstadoPregunta.PENDIENTE_REVISION
                        || p.getEstado() == EstadoPregunta.EN_REVISION)
                .toList();
    }
}
