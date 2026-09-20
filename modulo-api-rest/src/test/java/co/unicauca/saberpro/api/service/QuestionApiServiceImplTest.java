package co.unicauca.saberpro.api.service;

import co.unicauca.saberpro.api.dto.QuestionRequest;
import co.unicauca.saberpro.api.dto.QuestionResponse;
import co.unicauca.saberpro.preguntas.domain.Competencia;
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
        return new Question(id, "Nombre " + id, "Enunciado " + id,
                new QuestionDistractors("A1", "B1", "C1", "D1"),
                'B', EstadoPregunta.PENDIENTE_REVISION, Competencia.INGLES, "Tema", Dificultad.BASICO);
    }

    private static QuestionRequest solicitudConEspacios() {
        return new QuestionRequest("  Nombre  ", " Enunciado ", " A1 ", " B1 ", " C1 ", " D1 ",
                " b ", Competencia.INGLES, "  Tema ", Dificultad.BASICO);
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
        assertEquals("B", respuesta.respuestaCorrecta());
    }

    @Test
    void findByIdPropagaLaExcepcionCuandoNoExiste() {
        when(questionService.obtenerPregunta("P-404")).thenThrow(new NoSuchElementException("No existe"));

        assertThrows(NoSuchElementException.class, () -> service.findById("P-404"));
    }

    @Test
    void saveLimpiaEspaciosYDelegaLaCreacionAlDominio() {
        when(questionService.crearPregunta(any(), any(), any(), eq('b'), any(), any(), any()))
                .thenReturn(pregunta("P-013"));

        QuestionResponse respuesta = service.save(solicitudConEspacios());

        ArgumentCaptor<QuestionDistractors> opciones = ArgumentCaptor.forClass(QuestionDistractors.class);
        verify(questionService).crearPregunta(eq("Nombre"), eq("Enunciado"), opciones.capture(), eq('b'),
                eq(Competencia.INGLES), eq("Tema"), eq(Dificultad.BASICO));
        assertEquals("A1", opciones.getValue().getOpcionA());
        assertEquals("D1", opciones.getValue().getOpcionD());
        assertEquals("P-013", respuesta.id());
    }

    @Test
    void updateDelegaLaActualizacionYDevuelveLaPreguntaGuardada() {
        when(questionService.obtenerPregunta("P-001")).thenReturn(pregunta("P-001"));

        QuestionResponse respuesta = service.update("P-001", solicitudConEspacios());

        verify(questionService).actualizarContenido(eq("P-001"), eq("Nombre"), eq("Enunciado"),
                any(QuestionDistractors.class), eq('b'), eq(Competencia.INGLES), eq("Tema"),
                eq(Dificultad.BASICO));
        assertEquals("P-001", respuesta.id());
    }

    @Test
    void updatePropagaLaExcepcionCuandoLaPreguntaNoExiste() {
        doThrow(new NoSuchElementException("No existe")).when(questionService)
                .actualizarContenido(eq("P-404"), any(), any(), any(), eq('b'), any(), any(), any());

        assertThrows(NoSuchElementException.class, () -> service.update("P-404", solicitudConEspacios()));
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
