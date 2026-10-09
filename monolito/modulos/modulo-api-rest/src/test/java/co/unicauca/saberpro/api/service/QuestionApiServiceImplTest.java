package co.unicauca.saberpro.api.service;

import co.unicauca.saberpro.api.dto.QuestionRequest;
import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.preguntas.domain.Competencia;
import co.unicauca.saberpro.preguntas.domain.ContenidoPregunta;
import co.unicauca.saberpro.preguntas.domain.Dificultad;
import co.unicauca.saberpro.preguntas.domain.EstadoPregunta;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionDistractors;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionApiServiceImplTest {

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionApiServiceImpl service;

    private static Question pregunta(String id) {
        return Question.builder().id(id).nombre("Nombre " + id).contexto("Contexto " + id)
                .enunciado("¿Enunciado " + id + "?").opciones(new QuestionDistractors("A1", "B1", "C1", "D1"))
                .respuestaCorrecta('B').justificacion("Porque sí").bibliografia("Libro")
                .estado(EstadoPregunta.BORRADOR).competencia(Competencia.INGLES).tema("Tema").subtema("Subtema")
                .dificultad(Dificultad.BASICO).autor("autor1").build();
    }

    private static QuestionRequest solicitud() {
        return new QuestionRequest("Nombre", "Contexto", "¿Enunciado?", "Aa1", "Bb2", "Cc3", "Dd4", "b",
                "Justificación", "Libro", Competencia.INGLES, "Tema", "Subtema", Dificultad.BASICO, "autor1");
    }

    @Test
    void findAllConvierteCadaPreguntaDelDominioEnRespuesta() {
        when(questionService.listarPreguntas()).thenReturn(List.of(pregunta("P-001"), pregunta("P-002")));

        List<QuestionResponse> respuestas = service.findAll();

        assertEquals(2, respuestas.size());
        assertEquals("P-001", respuestas.get(0).id());
        assertEquals("P-002", respuestas.get(1).id());
    }

    @Test
    void findByIdDevuelveLaPreguntaConvertida() {
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta("P-001"));

        QuestionResponse respuesta = service.findById("P-001");

        assertEquals("Nombre P-001", respuesta.nombre());
        assertEquals("Contexto P-001", respuesta.contexto());
        assertEquals("B", respuesta.respuestaCorrecta());
        assertEquals("autor1", respuesta.autor());
    }

    @Test
    void findByIdPropagaLaExcepcionCuandoNoExiste() {
        when(questionService.obtenerPregunta("P-404")).thenThrow(new NoSuchElementException("No existe"));

        assertThrows(NoSuchElementException.class, () -> service.findById("P-404"));
    }

    @Test
    void saveCreaUnBorradorATravesDelDominioConElAutorDeLaSolicitud() {
        when(questionService.crearBorrador(any(ContenidoPregunta.class), eq("autor1"))).thenReturn(pregunta("P-013"));

        QuestionResponse respuesta = service.save(solicitud());

        ArgumentCaptor<ContenidoPregunta> contenido = ArgumentCaptor.forClass(ContenidoPregunta.class);
        verify(questionService).crearBorrador(contenido.capture(), eq("autor1"));
        assertEquals("Contexto", contenido.getValue().contexto());
        assertEquals("Dd4", contenido.getValue().opcionD());
        assertEquals("Subtema", contenido.getValue().subtema());
        assertEquals("P-013", respuesta.id());
    }

    @Test
    void updateDelegaLaActualizacionYDevuelveLaPreguntaGuardada() {
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta("P-001"));

        QuestionResponse respuesta = service.update("P-001", solicitud());

        verify(questionService).actualizarContenido(eq("P-001"), any(ContenidoPregunta.class), eq("autor1"));
        assertEquals("P-001", respuesta.id());
    }

    @Test
    void updatePropagaLaExcepcionCuandoLaPreguntaNoExiste() {
        doThrow(new NoSuchElementException("No existe")).when(questionService)
                .actualizarContenido(eq("P-404"), any(ContenidoPregunta.class), eq("autor1"));

        assertThrows(NoSuchElementException.class, () -> service.update("P-404", solicitud()));
    }

    @Test
    void deleteArchivaLaPreguntaEnVezDeBorrarla() {
        service.delete("P-001");

        verify(questionService).cambiarEstado("P-001", EstadoPregunta.ARCHIVADA);
    }

    @Test
    void deletePropagaLaExcepcionCuandoLaPreguntaNoExiste() {
        doThrow(new NoSuchElementException("No existe")).when(questionService)
                .cambiarEstado("P-404", EstadoPregunta.ARCHIVADA);

        assertThrows(NoSuchElementException.class, () -> service.delete("P-404"));
    }
}
