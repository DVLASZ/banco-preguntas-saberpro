package co.unicauca.saberpro.preguntas.access;

import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionImplRepositoryTest {

    private QuestionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new QuestionImplRepository();
    }

    @Test
    void obtenerTodas_retornaLosDatosDeEjemploPrecargados() {
        assertFalse(repository.obtenerTodas().isEmpty());
    }

    @Test
    void obtenerPorId_retornaLaPreguntaCorrecta() {
        Question pregunta = repository.obtenerPorId("P-001");

        assertNotNull(pregunta);
        assertEquals("P-001", pregunta.getId());
    }

    @Test
    void obtenerPorId_retornaNuloSiNoExiste() {
        assertNull(repository.obtenerPorId("NO-EXISTE"));
    }

    @Test
    void actualizar_persisteElCambioDeEstado() {
        Question pregunta = repository.obtenerPorId("P-001");
        pregunta.setEstado(EstadoPregunta.PUBLICADA);

        repository.actualizar(pregunta);

        assertEquals(EstadoPregunta.PUBLICADA, repository.obtenerPorId("P-001").getEstado());
    }

    @Test
    void actualizar_rechazaPreguntaConIdDesconocido() {
        Question desconocida = new Question("P-999", "n", "e",
                pregunta().getOpciones(), 'A', EstadoPregunta.BORRADOR,
                Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO);

        assertThrows(IllegalArgumentException.class, () -> repository.actualizar(desconocida));
    }

    @Test
    void generarNuevoId_continuaDespuesDeLosDatosDeEjemplo() {
        // Hay 12 preguntas de ejemplo precargadas (P-001..P-012, ver
        // QuestionImplRepository.cargarDatosDeEjemplo), así que el
        // siguiente id generado debe continuar desde ahí.
        assertEquals("P-013", repository.generarNuevoId());
        assertEquals("P-014", repository.generarNuevoId());
    }

    @Test
    void crear_agregaUnaPreguntaNueva() {
        Question nueva = new Question("P-999", "n", "e", pregunta().getOpciones(), 'A',
                EstadoPregunta.PENDIENTE_REVISION, Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO);

        repository.crear(nueva);

        assertSame(nueva, repository.obtenerPorId("P-999"));
    }

    @Test
    void crear_rechazaUnIdYaExistente() {
        Question duplicada = new Question("P-001", "n", "e", pregunta().getOpciones(), 'A',
                EstadoPregunta.BORRADOR, Competencia.LECTURA_CRITICA, "tema", Dificultad.BASICO);

        assertThrows(IllegalArgumentException.class, () -> repository.crear(duplicada));
    }

    private Question pregunta() {
        return repository.obtenerPorId("P-001");
    }
}
