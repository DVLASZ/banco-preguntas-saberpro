package co.unicauca.bancopreguntas.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Registro de servicios. Cada réplica de cada microservicio se anuncia aquí al arrancar y el gateway consulta
 * el registro para repartir las peticiones entre las réplicas que estén sanas.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
