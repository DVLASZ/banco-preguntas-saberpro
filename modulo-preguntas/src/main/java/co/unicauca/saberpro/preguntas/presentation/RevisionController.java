package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.FuenteDePreguntasParaRevisar;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;

/**
 * Controlador (MVC) de la ventana del Revisor (RF-16): toma las preguntas que
 * le corresponden de una {@link FuenteDePreguntasParaRevisar}, muestra la que
 * elige y registra su decisión (aprobar o rechazar) en el modelo
 * ({@link QuestionService}), que valida la transición y avisa a los observadores.
 */
public class RevisionController {

    private final QuestionService modelo;
    private final FuenteDePreguntasParaRevisar fuente;
    private final String usuario;
    private final RevisionVista vista;

    private Question preguntaCargada;

    public RevisionController(QuestionService modelo, FuenteDePreguntasParaRevisar fuente, String usuario,
                              RevisionVista vista) {
        this.modelo = modelo;
        this.fuente = fuente;
        this.usuario = usuario;
        this.vista = vista;
    }

    public void iniciar() {
        vista.mostrarPreguntasPorRevisar(fuente.paraRevisor(usuario));
    }

    /** El Revisor eligió una pregunta para evaluarla. */
    public void cargarPregunta(String id) {
        preguntaCargada = modelo.obtenerPregunta(id);

        // Al tomar una pregunta pendiente para revisarla, pasa
        // automáticamente a "En revisión" (RF-15): el solo hecho de que
        // el Revisor la abra ya inicia su evaluación.
        if (preguntaCargada.getEstado() == EstadoPregunta.PENDIENTE_REVISION) {
            modelo.cambiarEstado(preguntaCargada.getId(), EstadoPregunta.EN_REVISION);
            preguntaCargada = modelo.obtenerPregunta(preguntaCargada.getId());
        }

        // Solo una pregunta en revisión se puede aprobar o rechazar (RF-15).
        vista.mostrarPregunta(preguntaCargada, preguntaCargada.getEstado() == EstadoPregunta.EN_REVISION);
    }

    public void aprobar() {
        decidir(EstadoPregunta.APROBADA);
    }

    public void rechazar() {
        decidir(EstadoPregunta.RECHAZADA);
    }

    private void decidir(EstadoPregunta decision) {
        if (preguntaCargada == null) {
            return;
        }
        try {
            modelo.cambiarEstado(preguntaCargada.getId(), decision);
        } catch (RuntimeException ex) {
            vista.mostrarError("No se pudo registrar la revisión", ex.getMessage());
            return;
        }
        vista.mostrarDecision(preguntaCargada.getId(), decision);
    }
}
