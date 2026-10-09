package co.unicauca.saberpro.api.integration;

import co.unicauca.saberpro.api.config.SampleDataSeeder;
import co.unicauca.saberpro.api.repository.QuestionJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "banco.seed.enabled=true")
class SampleDataSeederTest {

    @Autowired
    private QuestionJpaRepository jpa;

    @Autowired
    private SampleDataSeeder seeder;

    @Test
    void alArrancarCarga12PreguntasDeEjemploDelMonolito() {
        assertEquals(12, jpa.count());
        assertTrue(jpa.existsById("P-001"));
        assertTrue(jpa.existsById("P-012"));
    }

    @Test
    void laCargaInicialIncluyeElContenidoCompletoYElAutor() {
        var pregunta = jpa.findById("P-001").orElseThrow();

        assertEquals("autor1", pregunta.getAutor());
        assertTrue(!pregunta.getContexto().isBlank());
        assertTrue(!pregunta.getJustificacion().isBlank());
        assertTrue(!pregunta.getBibliografia().isBlank());
        assertTrue(!pregunta.getSubtema().isBlank());
    }

    @Test
    void ejecutarloOtraVezNoDuplicaLasPreguntas() {
        seeder.run(null);

        assertEquals(12, jpa.count());
    }
}
