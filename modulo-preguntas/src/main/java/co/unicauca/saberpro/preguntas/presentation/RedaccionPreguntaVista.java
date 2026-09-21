package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.validation.Violacion;

import java.util.List;

/**
 * Lo que el {@link RedaccionPreguntaController} necesita de la vista de
 * redacción del Autor: leer y pintar el formulario, avisar y preguntar al
 * usuario. No conoce Swing, así que el controlador se prueba con una vista falsa.
 */
public interface RedaccionPreguntaVista {

    /** El contenido tal como está escrito ahora en el formulario. */
    ContenidoPregunta leerFormulario();

    /** Pinta una pregunta guardada; solo se puede modificar si {@code editable}. */
    void mostrarPregunta(Question pregunta, boolean editable, String aviso);

    /** Deja el formulario vacío y editable para redactar una pregunta nueva. */
    void mostrarFormularioNuevo(String aviso);

    /** Deja el formulario vacío y bloqueado, mientras no hay ninguna pregunta abierta. */
    void mostrarSinSeleccion(String aviso);

    void limpiarResaltados();

    /** Resalta los campos que incumplen la validación y le dice al usuario qué corregir. */
    void mostrarViolaciones(List<Violacion> violaciones);

    void mostrarError(String titulo, String mensaje);

    void informar(String titulo, String mensaje);

    /** Pregunta si de verdad quiere enviar la pregunta a revisión (ya no podrá modificarla). */
    boolean confirmarEnvio();

    /** Pregunta si quiere descartar los cambios sin guardar. */
    boolean confirmarDescarte();

    /** Vuelve a consultar el listado "Mis preguntas" (tras guardar o enviar una pregunta). */
    void actualizarListado();

    /** Marca en el listado la pregunta abierta, o deja de marcar cualquiera si es {@code null}. */
    void marcarEnListado(String idPregunta);
}
