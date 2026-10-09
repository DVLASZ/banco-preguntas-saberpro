package co.unicauca.bancopreguntas.platform.api;

import co.unicauca.bancopreguntas.platform.security.AuthenticatedUser;
import co.unicauca.bancopreguntas.platform.security.ForbiddenException;
import co.unicauca.bancopreguntas.platform.security.HeaderUserArgumentResolver;
import co.unicauca.bancopreguntas.platform.security.Roles;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PlatformExceptionHandler")
class PlatformExceptionHandlerTest {

    record Cuerpo(@NotBlank(message = "es obligatorio") String nombre) {
    }

    @RestController
    static class ControladorDePrueba {

        @PostMapping("/validar")
        String validar(@Valid @RequestBody Cuerpo cuerpo) {
            return "ok";
        }

        @GetMapping("/restricciones")
        String restricciones() {
            throw new ConstraintViolationException("fallo", Set.of());
        }

        @GetMapping("/prohibido")
        String prohibido() {
            throw new ForbiddenException("No tienes permiso");
        }

        @GetMapping("/solo-admin")
        String soloAdmin(AuthenticatedUser usuario) {
            usuario.requireAnyRole(Roles.ADMIN);
            return "ok";
        }

        @GetMapping("/inesperado")
        String inesperado() {
            throw new IllegalStateException("detalle interno secreto");
        }

        @GetMapping("/no-encontrado")
        String noEncontrado() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no existe");
        }

        @GetMapping("/tipo/{n}")
        String tipo(@org.springframework.web.bind.annotation.PathVariable int n) {
            return "ok";
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new PlatformExceptionHandler(), new PlatformFallbackExceptionHandler())
                .setCustomArgumentResolvers(new HeaderUserArgumentResolver())
                .setValidator(validador)
                .build();
    }

    @Test
    @DisplayName("un campo inválido responde 400 con el campo en los detalles")
    void validacionDeEntrada() throws Exception {
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_ENTRADA"))
                .andExpect(jsonPath("$.detalles[0]").value("nombre: es obligatorio"));
    }

    @Test
    @DisplayName("un cuerpo que no es JSON responde 400")
    void cuerpoMalFormado() throws Exception {
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }

    @Test
    @DisplayName("un parámetro de ruta con tipo incorrecto responde 400")
    void tipoIncorrecto() throws Exception {
        mvc.perform(get("/tipo/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }

    @Test
    @DisplayName("una violación de restricciones responde 400")
    void restricciones() throws Exception {
        mvc.perform(get("/restricciones"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("sin identidad responde 401")
    void sinIdentidad() throws Exception {
        mvc.perform(get("/solo-admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("con un rol insuficiente responde 403")
    void rolInsuficiente() throws Exception {
        mvc.perform(get("/solo-admin").header("X-User-Id", "u-1").header("X-User-Roles", "AUTHOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        mvc.perform(get("/prohibido"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("con el rol correcto la petición pasa")
    void rolCorrecto() throws Exception {
        mvc.perform(get("/solo-admin").header("X-User-Id", "u-1").header("X-User-Roles", "admin"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un error inesperado responde 500 sin revelar detalles")
    void errorInesperado() throws Exception {
        mvc.perform(get("/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secreto"))));
    }

    @Test
    @DisplayName("los errores que Spring ya expresa con un código HTTP lo conservan")
    void conservanSuCodigo() throws Exception {
        mvc.perform(get("/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
        mvc.perform(put("/validar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.mensaje").value("El método HTTP no está permitido en esta ruta"));
    }
}
