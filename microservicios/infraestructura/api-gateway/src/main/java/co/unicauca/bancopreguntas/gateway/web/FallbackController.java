package co.unicauca.bancopreguntas.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Respuesta cuando un servicio no responde o su circuito está abierto: 503 inmediato, con el formato de error común. */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{servicio}")
    public ResponseEntity<ApiError> servicioNoDisponible(@PathVariable String servicio) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError.de("SERVICIO_NO_DISPONIBLE",
                "El servicio " + servicio + " no está disponible en este momento; intenta de nuevo en unos segundos"));
    }
}
