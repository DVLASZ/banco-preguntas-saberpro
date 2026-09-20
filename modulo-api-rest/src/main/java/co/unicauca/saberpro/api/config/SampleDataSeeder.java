package co.unicauca.saberpro.api.config;

import co.unicauca.saberpro.api.repository.QuestionJpaAdapter;
import co.unicauca.saberpro.api.repository.QuestionJpaRepository;
import co.unicauca.saberpro.preguntas.access.QuestionImplRepository;
import co.unicauca.saberpro.preguntas.domain.Question;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Si la tabla esta vacia, la llena con las mismas preguntas de ejemplo del
 * monolito, para que el primer GET de la API ya devuelva datos.
 */
@Component
@ConditionalOnProperty(name = "banco.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SampleDataSeeder implements ApplicationRunner {

    private final QuestionJpaRepository jpa;
    private final QuestionJpaAdapter adapter;

    public SampleDataSeeder(QuestionJpaRepository jpa, QuestionJpaAdapter adapter) {
        this.jpa = jpa;
        this.adapter = adapter;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (jpa.count() > 0) {
            return;
        }
        for (Question pregunta : new QuestionImplRepository().obtenerTodas()) {
            adapter.crear(pregunta);
        }
    }
}
