package co.unicauca.bancopreguntas.question;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exporta el contrato OpenAPI de {@code question-service} a un archivo, tal como lo sirve {@code /v3/api-docs}. Solo corre cuando
 * se pide con {@code -Dopenapi.export=<carpeta>}; así se regeneran los archivos de {@code docs/corte-2/api/openapi}:
 *
 * <pre>./mvnw -pl servicios/question-service -am test -Dtest=ContratoOpenApiTest -Dopenapi.export=ruta/a/openapi</pre>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "management.health.rabbit.enabled=false"
        })
@Import(EmbeddedPostgresConfig.class)
@EnabledIfSystemProperty(named = "openapi.export", matches = ".+")
@DisplayName("Contrato OpenAPI de question-service")
class ContratoOpenApiTest {

    @Autowired
    private TestRestTemplate http;

    @MockitoBean
    private RabbitTemplate rabbit;

    @Test
    @DisplayName("se exporta tal como lo sirve el servicio")
    void exporta() throws Exception {
        String contrato = http.getForObject("/v3/api-docs", String.class);

        assertThat(contrato).contains("\"openapi\"");
        Path carpeta = Path.of(System.getProperty("openapi.export"));
        Files.createDirectories(carpeta);
        Files.writeString(carpeta.resolve("openapi-question-service.json"), contrato, StandardCharsets.UTF_8);
    }
}
