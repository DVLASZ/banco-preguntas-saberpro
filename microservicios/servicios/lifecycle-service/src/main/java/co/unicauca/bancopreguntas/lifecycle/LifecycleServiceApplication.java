package co.unicauca.bancopreguntas.lifecycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del microservicio del ciclo de vida de las preguntas. */
@SpringBootApplication
public class LifecycleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifecycleServiceApplication.class, args);
    }
}
