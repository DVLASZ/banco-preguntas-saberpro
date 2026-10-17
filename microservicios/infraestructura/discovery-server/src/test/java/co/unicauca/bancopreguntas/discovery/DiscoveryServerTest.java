package co.unicauca.bancopreguntas.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("discovery-server (Eureka)")
class DiscoveryServerTest {

    @Autowired
    private TestRestTemplate http;

    private ResponseEntity<String> json(String ruta) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setAccept(List.of(MediaType.APPLICATION_JSON));
        return http.exchange(ruta, HttpMethod.GET, new HttpEntity<>(cabeceras), String.class);
    }

    @Test
    @DisplayName("arranca y está sano")
    void salud() {
        ResponseEntity<String> r = json("/actuator/health");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("UP");
    }

    @Test
    @DisplayName("expone el registro de servicios, vacío al empezar")
    void registro() {
        ResponseEntity<String> r = json("/eureka/apps");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("applications");
    }

    @Test
    @DisplayName("un servicio puede anunciarse y luego darse de baja")
    void registraYDaDeBaja() {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        cabeceras.setAccept(List.of(MediaType.APPLICATION_JSON));
        String instancia = """
                {"instance":{"instanceId":"prueba:1","hostName":"localhost","app":"PRUEBA","ipAddr":"127.0.0.1",
                 "status":"UP","port":{"$":9999,"@enabled":"true"},
                 "dataCenterInfo":{"@class":"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo","name":"MyOwn"}}}
                """;

        ResponseEntity<String> alta = http.exchange("/eureka/apps/PRUEBA", HttpMethod.POST,
                new HttpEntity<>(instancia, cabeceras), String.class);
        assertThat(alta.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(json("/eureka/apps/PRUEBA").getBody()).contains("prueba:1");

        http.exchange("/eureka/apps/PRUEBA/prueba:1", HttpMethod.DELETE, new HttpEntity<>(cabeceras), String.class);
        assertThat(json("/eureka/apps/PRUEBA").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
