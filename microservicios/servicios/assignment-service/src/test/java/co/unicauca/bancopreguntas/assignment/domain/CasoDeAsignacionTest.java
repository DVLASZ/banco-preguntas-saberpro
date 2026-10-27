package co.unicauca.bancopreguntas.assignment.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CasoDeAsignacion (agregado)")
class CasoDeAsignacionTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final Instant T2 = T0.plusSeconds(120);

    private CasoDeAsignacion caso;

    @BeforeEach
    void abrirCaso() {
        caso = CasoDeAsignacion.nuevo("c-1", "q-1", "autor", "Patrón Observer", "LECTURA_CRITICA", T0);
    }

    private void conRevisores(String... revisores) {
        caso.iniciarRonda(1, T0);
        caso.asignarRevisores(List.of(revisores), "admin", 5, T0);
    }

    @Nested
    @DisplayName("iniciar una ronda")
    class IniciarRonda {

        @Test
        @DisplayName("la primera ronda queda pendiente de que el administrador asigne revisores")
        void primeraRonda() {
            RondaIniciada iniciada = caso.iniciarRonda(1, T0);

            assertThat(iniciada.nueva()).isTrue();
            assertThat(iniciada.conRevisoresReasignados()).isFalse();
            assertThat(iniciada.ronda().numero()).isEqualTo(1);
            assertThat(iniciada.ronda().versionDePregunta()).isEqualTo(1);
            assertThat(iniciada.ronda().estado()).isEqualTo(EstadoRonda.PENDIENTE_ASIGNACION);
            assertThat(iniciada.ronda().revisores()).isEmpty();
        }

        @Test
        @DisplayName("un evento repetido de la misma versión devuelve la ronda existente sin cambios")
        void idempotente() {
            caso.iniciarRonda(1, T0);

            RondaIniciada otra = caso.iniciarRonda(1, T1);

            assertThat(otra.nueva()).isFalse();
            assertThat(caso.rondas()).hasSize(1);
        }

        @Test
        @DisplayName("tras un rechazo, la siguiente ronda reasigna sola a los mismos revisores y arranca en curso")
        void reasignaSola() {
            conRevisores("r1", "r2");
            caso.registrarDecisionDeRonda(1, false, T1);

            RondaIniciada segunda = caso.iniciarRonda(2, T2);

            assertThat(segunda.nueva()).isTrue();
            assertThat(segunda.conRevisoresReasignados()).isTrue();
            assertThat(segunda.ronda().numero()).isEqualTo(2);
            assertThat(segunda.ronda().estado()).isEqualTo(EstadoRonda.EN_CURSO);
            assertThat(segunda.ronda().revisores()).containsExactly("r1", "r2");
            assertThat(segunda.ronda().asignaciones()).allMatch(a -> a.automatica() && a.asignadoPor().equals(Asignacion.SISTEMA));
            assertThat(caso.rondas().get(0).estado()).isEqualTo(EstadoRonda.CERRADA);
        }

        @Test
        @DisplayName("no se puede iniciar otra ronda mientras la anterior sigue en curso")
        void rondaEnCurso() {
            conRevisores("r1");

            assertThatThrownBy(() -> caso.iniciarRonda(2, T1)).isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("sigue abierta");
        }

        @Test
        @DisplayName("no se puede iniciar otra ronda sobre una pregunta ya aprobada")
        void yaAprobada() {
            conRevisores("r1");
            caso.registrarDecisionDeRonda(1, true, T1);

            assertThatThrownBy(() -> caso.iniciarRonda(2, T2)).isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("aprobada");
        }
    }

    @Nested
    @DisplayName("asignar revisores")
    class Asignar {

        @Test
        @DisplayName("la primera asignación pone la ronda en curso; con un solo revisor basta")
        void primeraAsignacion() {
            caso.iniciarRonda(1, T0);

            List<Asignacion> nuevas = caso.asignarRevisores(List.of("r1"), "admin", 3, T1);

            assertThat(nuevas).singleElement().satisfies(a -> {
                assertThat(a.revisorId()).isEqualTo("r1");
                assertThat(a.asignadoPor()).isEqualTo("admin");
                assertThat(a.automatica()).isFalse();
            });
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.EN_CURSO);
        }

        @Test
        @DisplayName("el administrador puede agregar revisores a una ronda en curso, sin quitar a nadie")
        void agregar() {
            conRevisores("r1");

            caso.asignarRevisores(List.of("r2"), "admin", 5, T1);

            assertThat(caso.rondaActual().revisores()).containsExactly("r1", "r2");
        }

        @Test
        @DisplayName("rechaza una lista vacía o nula, identificadores vacíos y repetidos")
        void listasInvalidas() {
            caso.iniciarRonda(1, T0);

            assertThatThrownBy(() -> caso.asignarRevisores(List.of(), "admin", 5, T1)).isInstanceOf(RevisionInvalidaException.class);
            assertThatThrownBy(() -> caso.asignarRevisores(null, "admin", 5, T1)).isInstanceOf(RevisionInvalidaException.class);
            assertThatThrownBy(() -> caso.asignarRevisores(List.of(" "), "admin", 5, T1)).isInstanceOf(RevisionInvalidaException.class);
            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r1", "r1"), "admin", 5, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("repetido");
        }

        @Test
        @DisplayName("el autor no puede ser su propio revisor ni se asigna dos veces a alguien")
        void autorYRepetidos() {
            conRevisores("r1");

            assertThatThrownBy(() -> caso.asignarRevisores(List.of("autor"), "admin", 5, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("propio revisor");
            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r1"), "admin", 5, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("ya está asignado");
        }

        @Test
        @DisplayName("respeta el máximo configurado, contando los ya asignados")
        void maximo() {
            conRevisores("r1", "r2");

            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r3", "r4"), "admin", 3, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("como máximo 3");
        }

        @Test
        @DisplayName("no se asigna a una ronda ya decidida, ni cuando no hay ronda")
        void rondaDecidida() {
            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r1"), "admin", 5, T1))
                    .isInstanceOf(OperacionNoPermitidaException.class);

            conRevisores("r1");
            caso.registrarDecisionDeRonda(1, false, T1);
            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r2"), "admin", 5, T2))
                    .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("RECHAZADA");
        }
    }

    @Nested
    @DisplayName("lo que informa evaluation-service")
    class Informes {

        @Test
        @DisplayName("la decisión de un revisor se recuerda una sola vez, y solo si está asignado a esa ronda")
        void registrarEvaluacion() {
            conRevisores("r1", "r2");

            assertThat(caso.registrarEvaluacion(1, "r1", Decision.APROBAR, T1)).isTrue();
            assertThat(caso.registrarEvaluacion(1, "r1", Decision.RECHAZAR, T2)).isFalse();
            assertThat(caso.registrarEvaluacion(1, "nadie", Decision.APROBAR, T2)).isFalse();
            assertThat(caso.registrarEvaluacion(7, "r2", Decision.APROBAR, T2)).isFalse();

            assertThat(caso.rondaActual().decisionDe("r1")).contains(Decision.APROBAR);
            assertThat(caso.rondaActual().decisionDe("r2")).isEmpty();
        }

        @Test
        @DisplayName("el resultado de la ronda la deja aprobada o rechazada, solo si estaba en curso")
        void registrarDecisionDeRonda() {
            conRevisores("r1");

            assertThat(caso.registrarDecisionDeRonda(1, false, T1)).isTrue();
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.RECHAZADA);
            assertThat(caso.rondaActual().decididaEn()).contains(T1);
            assertThat(caso.registrarDecisionDeRonda(1, true, T2)).isFalse();
            assertThat(caso.registrarDecisionDeRonda(9, true, T2)).isFalse();
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.RECHAZADA);
        }

        @Test
        @DisplayName("aprobar cierra la ronda al decidirla")
        void aprobada() {
            conRevisores("r1");

            caso.registrarDecisionDeRonda(1, true, T1);

            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.APROBADA);
            assertThat(caso.rondaActual().cerradaEn()).contains(T1);
        }
    }

    @Nested
    @DisplayName("reapertura")
    class Reapertura {

        @Test
        @DisplayName("solo cierra una ronda rechazada")
        void soloRechazada() {
            assertThat(caso.cerrarRondaPorReapertura(T1)).isFalse();
            conRevisores("r1");
            assertThat(caso.cerrarRondaPorReapertura(T1)).isFalse();

            caso.registrarDecisionDeRonda(1, false, T1);
            assertThat(caso.cerrarRondaPorReapertura(T2)).isTrue();
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.CERRADA);
            assertThat(caso.rondaActual().cerradaEn()).contains(T2);
        }

        @Test
        @DisplayName("reabrir dos veces es inofensivo")
        void dosVeces() {
            conRevisores("r1");
            caso.registrarDecisionDeRonda(1, false, T1);

            assertThat(caso.cerrarRondaPorReapertura(T2)).isTrue();
            assertThat(caso.cerrarRondaPorReapertura(T2)).isFalse();
        }
    }

    @Nested
    @DisplayName("construcción")
    class Construccion {

        @Test
        @DisplayName("exige identificador, pregunta y autor")
        void exige() {
            assertThatThrownBy(() -> CasoDeAsignacion.nuevo(" ", "q", "a", "t", "c", T0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CasoDeAsignacion.nuevo("c", null, "a", "t", "c", T0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CasoDeAsignacion.nuevo("c", "q", "", "t", "c", T0)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("un título o competencia ausentes quedan como texto vacío y las rondas no se modifican desde fuera")
        void valoresPorDefecto() {
            CasoDeAsignacion sin = CasoDeAsignacion.nuevo("c", "q", "a", null, null, T0);

            assertThat(sin.titulo()).isEmpty();
            assertThat(sin.competencia()).isEmpty();
            assertThatThrownBy(() -> sin.rondas().add(null)).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(sin::rondaActual).isInstanceOf(OperacionNoPermitidaException.class);
        }
    }
}
