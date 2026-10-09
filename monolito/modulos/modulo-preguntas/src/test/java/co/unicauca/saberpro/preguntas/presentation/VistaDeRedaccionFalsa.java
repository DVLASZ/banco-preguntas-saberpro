package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.validation.Violacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Vista de redacción sin ventana: guarda lo que el controlador le pide pintar y
 * responde a las preguntas con lo que la prueba haya dispuesto. Se comporta como
 * el formulario real: al mostrar una pregunta, el formulario pasa a tener su contenido.
 */
class VistaDeRedaccionFalsa implements RedaccionPreguntaVista {

    static final ContenidoPregunta VACIO =
            new ContenidoPregunta("", "", "", "", "", "", "", "", "", "", null, "", "", null);

    /** Lo que hay "escrito" en el formulario; la prueba lo cambia para simular que el usuario escribe. */
    ContenidoPregunta formulario = VACIO;

    boolean editable;
    String aviso;
    String idMostrado;
    String idMarcadoEnListado = "sin marcar";
    List<Violacion> violaciones = List.of();
    final List<String> errores = new ArrayList<>();
    final List<String> informes = new ArrayList<>();
    int actualizacionesDelListado;
    int preguntasDeConfirmacionDeDescarte;
    int preguntasDeConfirmacionDeEnvio;

    boolean respuestaAlEnvio = true;
    boolean respuestaAlDescarte = true;

    @Override
    public ContenidoPregunta leerFormulario() {
        return formulario;
    }

    @Override
    public void mostrarPregunta(Question pregunta, boolean editable, String aviso) {
        this.formulario = ContenidoPregunta.de(pregunta);
        this.idMostrado = pregunta.getId();
        this.editable = editable;
        this.aviso = aviso;
    }

    @Override
    public void mostrarFormularioNuevo(String aviso) {
        this.formulario = VACIO;
        this.idMostrado = null;
        this.editable = true;
        this.aviso = aviso;
    }

    @Override
    public void mostrarSinSeleccion(String aviso) {
        this.formulario = VACIO;
        this.idMostrado = null;
        this.editable = false;
        this.aviso = aviso;
    }

    @Override
    public void limpiarResaltados() {
        this.violaciones = List.of();
    }

    @Override
    public void mostrarViolaciones(List<Violacion> violaciones) {
        this.violaciones = violaciones;
    }

    @Override
    public void mostrarError(String titulo, String mensaje) {
        errores.add(titulo + ": " + mensaje);
    }

    @Override
    public void informar(String titulo, String mensaje) {
        informes.add(titulo + ": " + mensaje);
    }

    @Override
    public boolean confirmarEnvio() {
        preguntasDeConfirmacionDeEnvio++;
        return respuestaAlEnvio;
    }

    @Override
    public boolean confirmarDescarte() {
        preguntasDeConfirmacionDeDescarte++;
        return respuestaAlDescarte;
    }

    @Override
    public void actualizarListado() {
        actualizacionesDelListado++;
    }

    @Override
    public void marcarEnListado(String idPregunta) {
        this.idMarcadoEnListado = idPregunta;
    }
}
