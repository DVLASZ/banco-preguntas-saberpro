package co.unicauca.bancopreguntas.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Puerta de entrada del sistema. Valida el token, aplica los roles de cada ruta y reparte las peticiones entre
 * las réplicas de cada servicio que están registradas en Eureka.
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
