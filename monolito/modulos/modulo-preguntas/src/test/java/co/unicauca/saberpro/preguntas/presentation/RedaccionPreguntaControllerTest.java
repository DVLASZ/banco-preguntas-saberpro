package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.access.QuestionImplRepository;
import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import co.unicauca.saberpro.preguntas.domain.validation.ContenidoDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El controlador de la redacción del Autor, probado con una vista falsa y el banco de
 * ejemplo: autor1 tiene P-001 y P-002 en Borrador y P-003 pendiente de revisión.
 */
class RedaccionPreguntaControllerTest {

    private QuestionService modelo;
    private VistaDeRedaccionFalsa vista;
    private RedaccionPreguntaController controlador;

    @BeforeEach
    void setUp() {
        modelo = new QuestionService(new QuestionImplRepository());
        vista = new VistaDeRedaccionFalsa();
        controlador = new RedaccionPreguntaController(modelo, "autor1", vista);
        controlador.iniciar();
    }

    private static ContenidoPregunta contenidoValido() {
        return ContenidoDePrueba.valido().build();
    }

    // ---- abrir y crear ----

    @Test
    void iniciar_dejaElFormularioVacioYBloqueado() {
        assertFalse(vista.editable);
        assertEquals(RedaccionPreguntaController.AVISO_SIN_SELECCION, vista.aviso);
        assertNull(vista.idMarcadoEnListado);
    }

    @Test
    void abrirUnBorrador_lasMuestraEditables() {
        controlador.abrirPregunta("P-001");

        assertEquals("P-001", vista.idMostrado);
        assertTrue(vista.editable);
        assertEquals(RedaccionPreguntaController.AVISO_EDITABLE, vista.aviso);
        assertEquals("P-001", vista.idMarcadoEnListado);
        assertEquals("Pregunta sobre DDD", vista.formulario.nombre());
    }

    @Test
    void abrirUnaPreguntaQueNoEsBorrador_laMuestraDeSoloLectura() {
        controlador.abrirPregunta("P-003");

        assertFalse(vista.editable);
        assertEquals("Solo lectura: la pregunta está Pendiente de revisión.", vista.aviso);
    }

    @Test
    void nuevaPregunta_muestraUnFormularioVacioYEditable() {
        controlador.abrirPregunta("P-001");

        controlador.nuevaPregunta();

        assertTrue(vista.editable);
        assertNull(vista.idMostrado);
        assertNull(vista.idMarcadoEnListado);
        assertEquals(RedaccionPreguntaController.AVISO_NUEVA, vista.aviso);
        assertEquals(VistaDeRedaccionFalsa.VACIO, vista.formulario);
    }

    // ---- guardar (HU-01) ----

    @Test
    void guardarUnaPreguntaNueva_valida_creaElBorradorYLoAvisa() {
        int antes = modelo.listarPorAutor("autor1").size();
        controlador.nuevaPregunta();
        vista.formulario = contenidoValido();

        controlador.guardarBorrador();

        assertEquals(antes + 1, modelo.listarPorAutor("autor1").size());
        assertNotNull(vista.idMostrado);
        assertEquals(EstadoPregunta.BORRADOR, modelo.obtenerPregunta(vista.idMostrado).getEstado());
        assertEquals(1, vista.actualizacionesDelListado);
        assertEquals(1, vista.informes.size());
        assertTrue(vista.informes.get(0).contains("guardada como Borrador"));
        assertTrue(vista.violaciones.isEmpty());
    }

    @Test
    void guardarUnaPreguntaInvalida_noGuardaNadaYMuestraLosCamposAfectados() {
        int antes = modelo.listarPorAutor("autor1").size();
        controlador.nuevaPregunta();
        vista.formulario = ContenidoDePrueba.valido().nombre("").contexto("").build();

        controlador.guardarBorrador();

        assertEquals(antes, modelo.listarPorAutor("autor1").size());
        assertTrue(vista.informes.isEmpty());
        assertTrue(vista.violaciones.stream().anyMatch(v -> v.campo().equals("nombre")));
        assertTrue(vista.violaciones.stream().anyMatch(v -> v.campo().equals("contexto")));
    }

    @Test
    void guardarCambiosDeUnBorradorExistente_losActualiza() {
        controlador.abrirPregunta("P-001");
        vista.formulario = ContenidoDePrueba.valido().nombre("Nombre corregido").build();

        controlador.guardarBorrador();

        assertEquals("Nombre corregido", modelo.obtenerPregunta("P-001").getNombre());
        assertEquals("P-001", vista.idMostrado);
    }

    @Test
    void guardarUnaPreguntaDeOtroAutor_muestraElError() {
        RedaccionPreguntaController deOtroAutor = new RedaccionPreguntaController(modelo, "autor2", vista);
        deOtroAutor.iniciar();
        deOtroAutor.abrirPregunta("P-001");
        vista.formulario = ContenidoDePrueba.valido().nombre("Intento ajeno").build();

        deOtroAutor.guardarBorrador();

        assertEquals(1, vista.errores.size());
        assertTrue(vista.errores.get(0).startsWith("No se pudo guardar la pregunta"));
        assertEquals("Pregunta sobre DDD", modelo.obtenerPregunta("P-001").getNombre());
    }

    // ---- enviar a revisión (HU-02) ----

    @Test
    void enviarARevision_conConfirmacion_cambiaElEstadoYLaDejaDeSoloLectura() {
        controlador.abrirPregunta("P-001");

        controlador.enviarARevision();

        assertEquals(EstadoPregunta.PENDIENTE_REVISION, modelo.obtenerPregunta("P-001").getEstado());
        assertFalse(vista.editable);
        assertTrue(vista.informes.get(vista.informes.size() - 1).contains("Pendiente de revisión"));
    }

    @Test
    void enviarARevision_sinConfirmacion_laPreguntaSigueEnBorrador() {
        controlador.abrirPregunta("P-001");
        vista.respuestaAlEnvio = false;

        controlador.enviarARevision();

        assertEquals(1, vista.preguntasDeConfirmacionDeEnvio);
        assertEquals(EstadoPregunta.BORRADOR, modelo.obtenerPregunta("P-001").getEstado());
        assertTrue(vista.editable);
        assertTrue(vista.informes.isEmpty());
    }

    @Test
    void enviarARevision_siNoPasaLaValidacion_noCambiaElEstadoYDiceQueCorregir() {
        controlador.abrirPregunta("P-001");
        vista.formulario = ContenidoDePrueba.valido().opcionA("Observer").build();

        controlador.enviarARevision();

        assertEquals(EstadoPregunta.BORRADOR, modelo.obtenerPregunta("P-001").getEstado());
        assertFalse(vista.violaciones.isEmpty());
        assertTrue(vista.informes.isEmpty());
    }

    // ---- cancelar y cambios sin guardar ----

    @Test
    void cancelarSinCambios_noPreguntaNada() {
        controlador.abrirPregunta("P-001");

        controlador.cancelar();

        assertEquals(0, vista.preguntasDeConfirmacionDeDescarte);
        assertEquals("P-001", vista.idMostrado);
    }

    @Test
    void cancelarConCambios_pideConfirmacionYSiDiceQueNoConservaLoEscrito() {
        controlador.abrirPregunta("P-001");
        ContenidoPregunta editado = ContenidoDePrueba.valido().nombre("Cambio sin guardar").build();
        vista.formulario = editado;
        vista.respuestaAlDescarte = false;

        controlador.cancelar();

        assertEquals(1, vista.preguntasDeConfirmacionDeDescarte);
        assertEquals(editado, vista.formulario);
    }

    @Test
    void cancelarConCambios_siConfirmaVuelveALaPreguntaGuardada() {
        controlador.abrirPregunta("P-001");
        vista.formulario = ContenidoDePrueba.valido().nombre("Cambio sin guardar").build();

        controlador.cancelar();

        assertEquals("Pregunta sobre DDD", vista.formulario.nombre());
    }

    @Test
    void cancelarUnaPreguntaNuevaConfirmandoElDescarte_dejaElFormularioBloqueado() {
        controlador.nuevaPregunta();
        vista.formulario = contenidoValido();

        controlador.cancelar();

        assertFalse(vista.editable);
        assertEquals(RedaccionPreguntaController.AVISO_SIN_SELECCION, vista.aviso);
    }

    @Test
    void abrirOtraPreguntaConCambiosSinGuardar_siNoConfirmaSeQuedaEnLaActual() {
        controlador.abrirPregunta("P-001");
        vista.formulario = ContenidoDePrueba.valido().nombre("Cambio sin guardar").build();
        vista.respuestaAlDescarte = false;

        controlador.abrirPregunta("P-002");

        assertEquals("P-001", vista.idMostrado);
        assertEquals("P-001", vista.idMarcadoEnListado);
    }

    @Test
    void nuevaPreguntaConCambiosSinGuardar_pideConfirmacion() {
        controlador.abrirPregunta("P-001");
        vista.formulario = ContenidoDePrueba.valido().nombre("Cambio sin guardar").build();
        vista.respuestaAlDescarte = false;

        controlador.nuevaPregunta();

        assertEquals(1, vista.preguntasDeConfirmacionDeDescarte);
        assertEquals("P-001", vista.idMostrado);
    }

    @Test
    void enUnaPreguntaDeSoloLectura_noHayCambiosQueDescartar() {
        controlador.abrirPregunta("P-003");
        vista.formulario = ContenidoDePrueba.valido().nombre("No se puede editar").build();

        controlador.abrirPregunta("P-001");

        assertEquals(0, vista.preguntasDeConfirmacionDeDescarte);
        assertEquals("P-001", vista.idMostrado);
    }
}
