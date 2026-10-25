package co.unicauca.bancopreguntas.email.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Correo y CorreoEnviado")
class CorreoEnviadoTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Correo CORREO = new Correo("autor@correo.test", "Asunto", "Cuerpo");

    @Test
    @DisplayName("un correo enviado conserva el contenido y no tiene error")
    void enviado() {
        CorreoEnviado c = CorreoEnviado.enviado("c-1", "n-1", "autor", CORREO, T0);

        assertThat(c.estado()).isEqualTo(EstadoCorreo.ENVIADO);
        assertThat(c.error()).isNull();
        assertThat(c.para()).isEqualTo("autor@correo.test");
        assertThat(c.asunto()).isEqualTo("Asunto");
        assertThat(c.cuerpo()).isEqualTo("Cuerpo");
        assertThat(c.intentadoEn()).isEqualTo(T0);
    }

    @Test
    @DisplayName("un correo fallido dice por qué, aunque el motivo llegue vacío")
    void fallido() {
        assertThat(CorreoEnviado.fallido("c-1", "n-1", "autor", CORREO, "servidor caído", T0).error()).isEqualTo("servidor caído");
        assertThat(CorreoEnviado.fallido("c-1", "n-1", "autor", CORREO, null, T0).error()).isEqualTo("Error desconocido");
        assertThat(CorreoEnviado.fallido("c-1", "n-1", "autor", CORREO, "  ", T0).estado()).isEqualTo(EstadoCorreo.FALLIDO);
    }

    @Test
    @DisplayName("exige identificador, destinatario, estado, fecha y, si falló, el motivo")
    void obligatorios() {
        assertThatThrownBy(() -> new Correo(" ", "a", "b")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CorreoEnviado.enviado(" ", "n", "u", CORREO, T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorreoEnviado("c", "n", "u", "p@c.test", "a", "b", null, null, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorreoEnviado("c", "n", "u", "p@c.test", "a", "b", EstadoCorreo.ENVIADO, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorreoEnviado("c", "n", "u", "p@c.test", "a", "b", EstadoCorreo.FALLIDO, null, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("la página nunca tiene menos de una página y no se modifica desde fuera")
    void pagina() {
        Pagina<String> vacia = new Pagina<>(List.of(), 1, 10, 0);
        Pagina<String> trece = new Pagina<>(List.of("a"), 2, 10, 13);

        assertThat(vacia.totalPaginas()).isEqualTo(1);
        assertThat(trece.totalPaginas()).isEqualTo(2);
        assertThatThrownBy(() -> trece.elementos().add("b")).isInstanceOf(UnsupportedOperationException.class);
    }
}
