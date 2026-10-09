package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import co.unicauca.saberpro.preguntas.domain.validation.QuestionValidationException;

/**
 * Controlador (MVC) de la redacción de preguntas del Autor (HU-01, HU-02):
 * decide qué pasa cuando el usuario abre, guarda, envía o cancela, apoyándose
 * en el modelo ({@link QuestionService}) y dejando que la vista solo pinte y
 * pregunte. Recuerda qué pregunta se está editando y cómo estaba el formulario
 * al cargarla, para poder avisar de los cambios sin guardar.
 *
 * <p>Solo se puede modificar una pregunta mientras esté en Borrador (RF-06);
 * en cualquier otro estado el formulario queda de solo lectura.
 */
public class RedaccionPreguntaController {

    static final String AVISO_SIN_SELECCION = "Seleccione una pregunta o cree una nueva.";
    static final String AVISO_NUEVA = "Complete todos los campos y guarde el borrador.";
    static final String AVISO_EDITABLE = "Puede modificarla mientras esté en Borrador.";

    private final QuestionService modelo;
    private final String usuario;
    private final RedaccionPreguntaVista vista;

    /** {@code null} mientras se redacta una pregunta nueva aún no guardada. */
    private String idEnEdicion;
    private boolean edicionPermitida;
    /** Contenido tal como se cargó o se guardó, para detectar cambios sin guardar. */
    private ContenidoPregunta instantanea;

    public RedaccionPreguntaController(QuestionService modelo, String usuario, RedaccionPreguntaVista vista) {
        this.modelo = modelo;
        this.usuario = usuario;
        this.vista = vista;
    }

    /** Estado inicial: ninguna pregunta abierta. */
    public void iniciar() {
        mostrarSinSeleccion();
    }

    /** El usuario quiere redactar una pregunta nueva. */
    public void nuevaPregunta() {
        if (!puedeDescartarLoEscrito()) {
            return;
        }
        vista.limpiarResaltados();
        vista.marcarEnListado(null);
        vista.mostrarFormularioNuevo(AVISO_NUEVA);
        idEnEdicion = null;
        edicionPermitida = true;
        instantanea = vista.leerFormulario();
    }

    /** El usuario eligió una pregunta del listado para verla o editarla. */
    public void abrirPregunta(String id) {
        if (!puedeDescartarLoEscrito()) {
            vista.marcarEnListado(idEnEdicion);
            return;
        }
        mostrar(modelo.obtenerPregunta(id));
    }

    /** HU-01: guarda (o actualiza) el borrador aplicando la validación estructural. */
    public void guardarBorrador() {
        if (guardar()) {
            vista.informar("Borrador guardado", "Pregunta " + idEnEdicion + " guardada como Borrador.");
        }
    }

    /** HU-02: tras confirmar, guarda los cambios pendientes y envía la pregunta a revisión. */
    public void enviarARevision() {
        if (!vista.confirmarEnvio()) {
            return;
        }
        if (!guardar()) {
            return;
        }
        try {
            modelo.enviarARevision(idEnEdicion, usuario);
        } catch (QuestionValidationException ex) {
            vista.mostrarViolaciones(ex.getViolaciones());
            return;
        } catch (RuntimeException ex) {
            vista.mostrarError("No se pudo enviar a revisión", ex.getMessage());
            return;
        }
        String id = idEnEdicion;
        vista.informar("Pregunta enviada",
                "Pregunta " + id + " enviada a revisión: quedó en estado Pendiente de revisión.");
        recargar(id);
    }

    /** Cancela la edición: si hay cambios sin guardar pide confirmación antes de descartarlos. */
    public void cancelar() {
        if (!puedeDescartarLoEscrito()) {
            return;
        }
        vista.limpiarResaltados();
        if (idEnEdicion == null) {
            mostrarSinSeleccion();
        } else {
            mostrar(modelo.obtenerPregunta(idEnEdicion));
        }
    }

    /** @return {@code true} si la pregunta quedó guardada; si no, ya mostró qué corregir */
    private boolean guardar() {
        vista.limpiarResaltados();
        ContenidoPregunta contenido = vista.leerFormulario();
        try {
            if (idEnEdicion == null) {
                idEnEdicion = modelo.crearBorrador(contenido, usuario).getId();
            } else {
                modelo.actualizarContenido(idEnEdicion, contenido, usuario);
            }
        } catch (QuestionValidationException ex) {
            vista.mostrarViolaciones(ex.getViolaciones());
            return false;
        } catch (RuntimeException ex) {
            vista.mostrarError("No se pudo guardar la pregunta", ex.getMessage());
            return false;
        }
        recargar(idEnEdicion);
        return true;
    }

    private void recargar(String id) {
        vista.actualizarListado();
        mostrar(modelo.obtenerPregunta(id));
    }

    private void mostrar(Question pregunta) {
        boolean editable = pregunta.getEstado() == EstadoPregunta.BORRADOR;
        vista.limpiarResaltados();
        vista.marcarEnListado(pregunta.getId());
        vista.mostrarPregunta(pregunta, editable, editable ? AVISO_EDITABLE
                : "Solo lectura: la pregunta está " + pregunta.getEstado() + ".");
        idEnEdicion = pregunta.getId();
        edicionPermitida = editable;
        instantanea = vista.leerFormulario();
    }

    private void mostrarSinSeleccion() {
        vista.marcarEnListado(null);
        vista.mostrarSinSeleccion(AVISO_SIN_SELECCION);
        idEnEdicion = null;
        edicionPermitida = false;
        instantanea = vista.leerFormulario();
    }

    private boolean puedeDescartarLoEscrito() {
        return !edicionPermitida || vista.leerFormulario().equals(instantanea) || vista.confirmarDescarte();
    }
}
