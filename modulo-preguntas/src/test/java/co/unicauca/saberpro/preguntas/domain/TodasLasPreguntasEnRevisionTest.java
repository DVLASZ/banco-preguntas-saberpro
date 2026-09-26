package co.unicauca.saberpro.preguntas.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodasLasPreguntasEnRevisionTest {

    @Mock
    private QuestionService service;

    private static Question preguntaEn(String id, EstadoPregunta estado) {
        return new Question(id, "n", "¿e?", new QuestionDistractors("Aa1", "Bb2", "Cc3", "Dd4"), 'A', estado,
                Competencia.INGLES, "tema", Dificultad.BASICO);
    }

    @Test
    void devuelveSoloLasPreguntasPendientesOEnRevisionSinImportarElRevisor() {
        Question borrador = preguntaEn("P-001", EstadoPregunta.BORRADOR);
        Question pendiente = preguntaEn("P-002", EstadoPregunta.PENDIENTE_REVISION);
        Question enRevision = preguntaEn("P-003", EstadoPregunta.EN_REVISION);
        Question publicada = preguntaEn("P-004", EstadoPregunta.PUBLICADA);
        when(service.listarPreguntas()).thenReturn(List.of(borrador, pendiente, enRevision, publicada));

        List<Question> resultado = new TodasLasPreguntasEnRevision(service).paraRevisor("revisor1");

        assertEquals(List.of(pendiente, enRevision), resultado);
    }
}
