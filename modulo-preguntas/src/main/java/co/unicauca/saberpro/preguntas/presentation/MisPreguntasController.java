package co.unicauca.saberpro.preguntas.presentation;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.FiltroPreguntas;
import co.unicauca.saberpro.preguntas.domain.Pagina;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;

/**
 * Controlador (MVC) del listado "Mis preguntas" del Autor (HU-03): recuerda el
 * filtro, la página y el tamaño de página actuales, le pide al modelo
 * ({@link QuestionService}) la página que corresponde y se la entrega a la
 * vista. La vista solo le avisa qué hizo el usuario.
 */
public class MisPreguntasController {

    static final int TAMANO_INICIAL = 10;

    private final QuestionService modelo;
    private final String autor;
    private final MisPreguntasVista vista;

    private FiltroPreguntas filtro = FiltroPreguntas.sinFiltros();
    private int pagina = 1;
    private int tamano = TAMANO_INICIAL;
    private Pagina<Question> paginaActual;

    public MisPreguntasController(QuestionService modelo, String autor, MisPreguntasVista vista) {
        this.modelo = modelo;
        this.autor = autor;
        this.vista = vista;
    }

    /** Muestra la primera página, sin filtros. */
    public void iniciar() {
        filtro = FiltroPreguntas.sinFiltros();
        pagina = 1;
        refrescar();
    }

    /** Aplica un filtro nuevo y vuelve a la primera página. */
    public void filtrar(EstadoPregunta estado, Competencia competencia, String texto) {
        filtro = new FiltroPreguntas(estado, competencia, texto);
        pagina = 1;
        refrescar();
    }

    public void limpiarFiltros() {
        filtro = FiltroPreguntas.sinFiltros();
        pagina = 1;
        refrescar();
    }

    public void paginaSiguiente() {
        if (paginaActual.haySiguiente()) {
            pagina = paginaActual.numero() + 1;
            refrescar();
        }
    }

    public void paginaAnterior() {
        if (paginaActual.hayAnterior()) {
            pagina = paginaActual.numero() - 1;
            refrescar();
        }
    }

    /** Cambia cuántas preguntas se ven por página y vuelve a la primera. */
    public void cambiarTamanoDePagina(int nuevoTamano) {
        tamano = nuevoTamano;
        pagina = 1;
        refrescar();
    }

    /** Vuelve a consultar la página actual, por ejemplo tras guardar una pregunta. */
    public void refrescar() {
        paginaActual = modelo.buscarDelAutor(autor, filtro, pagina, tamano);
        pagina = paginaActual.numero();
        vista.mostrar(paginaActual);
    }

    public FiltroPreguntas filtroActual() {
        return filtro;
    }
}
