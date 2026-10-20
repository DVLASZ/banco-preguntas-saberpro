package co.unicauca.bancopreguntas.notification.infrastructure.messaging;

import co.unicauca.bancopreguntas.notification.application.port.EmailSender;
import co.unicauca.bancopreguntas.notification.domain.Correo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Decoradores del envío de correo")
class DecoradoresDeCorreoTest {

    private static final Correo CORREO = new Correo("autor@correo.test", "Asunto", "Cuerpo");

    /** Falla las primeras {@code fallos} veces y después acepta. */
    private static final class Inestable implements EmailSender {
        private final int fallos;
        private final List<Correo> enviados = new ArrayList<>();
        private int llamadas;

        Inestable(int fallos) {
            this.fallos = fallos;
        }

        @Override
        public void enviar(Correo correo) {
            llamadas++;
            if (llamadas <= fallos) {
                throw new IllegalStateException("servidor ocupado " + llamadas);
            }
            enviados.add(correo);
        }
    }

    @Test
    @DisplayName("con reintentos, un fallo pasajero no pierde el correo")
    void reintentaHastaQuePasa() {
        Inestable base = new Inestable(2);

        new EmailConReintentos(base, 3).enviar(CORREO);

        assertThat(base.llamadas).isEqualTo(3);
        assertThat(base.enviados).containsExactly(CORREO);
    }

    @Test
    @DisplayName("si tras todos los intentos sigue fallando, lanza la última excepción")
    void sinSuerte() {
        Inestable base = new Inestable(10);

        assertThatThrownBy(() -> new EmailConReintentos(base, 3).enviar(CORREO))
                .isInstanceOf(IllegalStateException.class).hasMessage("servidor ocupado 3");
        assertThat(base.llamadas).isEqualTo(3);
    }

    @Test
    @DisplayName("si el primer intento sale bien no se repite")
    void unSoloIntento() {
        Inestable base = new Inestable(0);

        new EmailConReintentos(base, 5).enviar(CORREO);

        assertThat(base.llamadas).isEqualTo(1);
    }

    @Test
    @DisplayName("exige al menos un intento")
    void intentosValidos() {
        assertThatThrownBy(() -> new EmailConReintentos(c -> { }, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el registro cuenta los envíos correctos y los fallidos y no oculta el error")
    void cuenta() {
        Inestable base = new Inestable(1);
        EmailConRegistro registro = new EmailConRegistro(base);

        assertThatThrownBy(() -> registro.enviar(CORREO)).isInstanceOf(IllegalStateException.class);
        registro.enviar(CORREO);
        registro.enviar(CORREO);

        assertThat(registro.fallidos()).isEqualTo(1);
        assertThat(registro.enviados()).isEqualTo(2);
    }

    @Test
    @DisplayName("los decoradores se pueden apilar: el registro ve el resultado final tras los reintentos")
    void apilados() {
        Inestable base = new Inestable(2);
        EmailConRegistro registro = new EmailConRegistro(new EmailConReintentos(base, 3));

        registro.enviar(CORREO);

        assertThat(registro.enviados()).isEqualTo(1);
        assertThat(registro.fallidos()).isZero();
        assertThat(base.llamadas).isEqualTo(3);
    }
}
