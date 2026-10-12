package co.unicauca.bancopreguntas.review.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CasoDeRevision (agregado)")
class CasoDeRevisionTest {

    private static final Instant T0 = Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);
    private static final Instant T2 = T0.plusSeconds(120);
    private static final PoliticaDeDecision POLITICA = new Unanimidad();

    private CasoDeRevision caso;

    @BeforeEach
    void abrirCaso() {
        caso = CasoDeRevision.nuevo("c-1", "q-1", "autor", "Patrón Observer", "LECTURA_CRITICA", T0);
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
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.PENDIENTE_ASIGNACION);
            assertThat(caso.rondaActual().numero()).isEqualTo(1);
        }

        @Test
        @DisplayName("un evento repetido de la misma versión devuelve la ronda existente sin cambios")
        void idempotentePorVersion() {
            caso.iniciarRonda(1, T0);

            RondaIniciada repetida = caso.iniciarRonda(1, T1);

            assertThat(repetida.nueva()).isFalse();
            assertThat(caso.rondas()).hasSize(1);
        }

        @Test
        @DisplayName("tras un rechazo, la siguiente ronda reasigna sola a los mismos revisores y arranca en curso")
        void reasignaLosMismos() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.RECHAZAR, "Falta contexto", POLITICA, T1);

            RondaIniciada segunda = caso.iniciarRonda(2, T2);

            assertThat(segunda.nueva()).isTrue();
            assertThat(segunda.conRevisoresReasignados()).isTrue();
            RondaDeRevision ronda = caso.rondaActual();
            assertThat(ronda.numero()).isEqualTo(2);
            assertThat(ronda.estado()).isEqualTo(EstadoRonda.EN_CURSO);
            assertThat(ronda.revisores()).containsExactly("r1", "r2");
            assertThat(ronda.asignaciones()).allMatch(a -> a.automatica() && a.asignadoPor().equals(Asignacion.SISTEMA));
            assertThat(ronda.evaluaciones()).isEmpty();
        }

        @Test
        @DisplayName("la ronda anterior rechazada queda cerrada al empezar la nueva")
        void cierraLaAnterior() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.RECHAZAR, "Mal", POLITICA, T1);

            caso.iniciarRonda(2, T2);

            RondaDeRevision primera = caso.rondas().get(0);
            assertThat(primera.estado()).isEqualTo(EstadoRonda.CERRADA);
            assertThat(primera.cerradaEn()).contains(T2);
        }

        @Test
        @DisplayName("no se puede iniciar otra ronda mientras la anterior sigue en curso")
        void rondaAbierta() {
            conRevisores("r1");

            assertThatThrownBy(() -> caso.iniciarRonda(2, T1))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("sigue abierta");
        }

        @Test
        @DisplayName("no se puede iniciar otra ronda sobre una pregunta ya aprobada")
        void yaAprobada() {
            conRevisores("r1");
            caso.evaluar("r1", Decision.APROBAR, null, POLITICA, T1);

            assertThatThrownBy(() -> caso.iniciarRonda(2, T2))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("aprobada");
        }
    }

    @Nested
    @DisplayName("asignar revisores")
    class Asignar {

        @Test
        @DisplayName("la primera asignación pone la ronda en curso")
        void ponEnCurso() {
            caso.iniciarRonda(1, T0);

            List<Asignacion> nuevas = caso.asignarRevisores(List.of("r1", "r2"), "admin", 5, T1);

            assertThat(nuevas).extracting(Asignacion::revisorId).containsExactly("r1", "r2");
            assertThat(nuevas).allMatch(a -> !a.automatica() && a.asignadoPor().equals("admin"));
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.EN_CURSO);
        }

        @Test
        @DisplayName("con un solo revisor basta (el mínimo es 1)")
        void unSoloRevisor() {
            caso.iniciarRonda(1, T0);

            caso.asignarRevisores(List.of("r1"), "admin", 5, T1);

            assertThat(caso.rondaActual().cantidadDeRevisores()).isEqualTo(1);
        }

        @Test
        @DisplayName("el administrador puede agregar revisores a una ronda en curso, sin quitar a nadie")
        void agregarMas() {
            conRevisores("r1", "r2");

            List<Asignacion> nuevas = caso.asignarRevisores(List.of("r3"), "admin", 5, T1);

            assertThat(nuevas).extracting(Asignacion::revisorId).containsExactly("r3");
            assertThat(caso.rondaActual().revisores()).containsExactly("r1", "r2", "r3");
        }

        @Test
        @DisplayName("al agregar un revisor la unanimidad ahora lo incluye")
        void elNuevoTambienDebeAprobar() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.APROBAR, null, POLITICA, T1);
            caso.asignarRevisores(List.of("r3"), "admin", 5, T1);

            ResultadoDeEvaluacion segunda = caso.evaluar("r2", Decision.APROBAR, null, POLITICA, T2);

            assertThat(segunda.decision()).isEqualTo(ResultadoDeDecision.PENDIENTE);
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.EN_CURSO);
        }

        @Test
        @DisplayName("rechaza una lista vacía o nula")
        void listaVacia() {
            caso.iniciarRonda(1, T0);

            assertThatThrownBy(() -> caso.asignarRevisores(List.of(), "admin", 5, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("al menos un revisor");
            assertThatThrownBy(() -> caso.asignarRevisores(null, "admin", 5, T1))
                    .isInstanceOf(RevisionInvalidaException.class);
        }

        @Test
        @DisplayName("el autor no puede ser su propio revisor")
        void autorComoRevisor() {
            caso.iniciarRonda(1, T0);

            assertThatThrownBy(() -> caso.asignarRevisores(List.of("autor"), "admin", 5, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("autor");
        }

        @Test
        @DisplayName("rechaza revisores repetidos en la solicitud o ya asignados")
        void repetidos() {
            conRevisores("r1");

            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r2", "r2"), "admin", 5, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("repetido");
            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r1"), "admin", 5, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("ya está asignado");
        }

        @Test
        @DisplayName("rechaza identificadores vacíos")
        void identificadorVacio() {
            caso.iniciarRonda(1, T0);

            assertThatThrownBy(() -> caso.asignarRevisores(java.util.Arrays.asList("r1", " "), "admin", 5, T1))
                    .isInstanceOf(RevisionInvalidaException.class);
        }

        @Test
        @DisplayName("respeta el máximo configurado, contando los ya asignados")
        void maximo() {
            conRevisores("r1", "r2", "r3");

            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r4", "r5"), "admin", 4, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("como máximo 4");
            assertThat(caso.rondaActual().cantidadDeRevisores()).isEqualTo(3);
        }

        @Test
        @DisplayName("no se asigna a una ronda ya decidida")
        void rondaDecidida() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.RECHAZAR, "Mal", POLITICA, T1);

            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r3"), "admin", 5, T2))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("sin ronda no hay a quién asignar")
        void sinRonda() {
            assertThatThrownBy(() -> caso.asignarRevisores(List.of("r1"), "admin", 5, T1))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }
    }

    @Nested
    @DisplayName("evaluar")
    class Evaluar {

        @Test
        @DisplayName("la unanimidad aprueba cuando todos aprueban")
        void apruebanTodos() {
            conRevisores("r1", "r2");

            ResultadoDeEvaluacion primera = caso.evaluar("r1", Decision.APROBAR, null, POLITICA, T1);
            ResultadoDeEvaluacion segunda = caso.evaluar("r2", Decision.APROBAR, "Bien", POLITICA, T2);

            assertThat(primera.decidioLaRonda()).isFalse();
            assertThat(segunda.decision()).isEqualTo(ResultadoDeDecision.APROBADA);
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.APROBADA);
            assertThat(caso.rondaActual().decididaEn()).contains(T2);
            assertThat(caso.rondaActual().cerradaEn()).contains(T2);
        }

        @Test
        @DisplayName("el primer rechazo decide la ronda sin esperar a los demás")
        void primerRechazo() {
            conRevisores("r1", "r2", "r3");

            ResultadoDeEvaluacion resultado = caso.evaluar("r2", Decision.RECHAZAR, "Ambigua", POLITICA, T1);

            assertThat(resultado.decision()).isEqualTo(ResultadoDeDecision.RECHAZADA);
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.RECHAZADA);
            assertThat(caso.rondaActual().decididaEn()).contains(T1);
            assertThat(caso.rondaActual().cerradaEn()).isEmpty();
        }

        @Test
        @DisplayName("tras el rechazo, un revisor pendiente deja una observación tardía que no cambia el resultado")
        void observacionTardia() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.RECHAZAR, "Mal", POLITICA, T1);

            ResultadoDeEvaluacion tardia = caso.evaluar("r2", Decision.APROBAR, "A mí me parece bien", POLITICA, T2);

            assertThat(tardia.evaluacion().tardia()).isTrue();
            assertThat(tardia.decidioLaRonda()).isFalse();
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.RECHAZADA);
            assertThat(caso.rondaActual().cantidadDeEvaluaciones()).isEqualTo(2);
        }

        @Test
        @DisplayName("una observación tardía que rechaza tampoco vuelve a decidir la ronda")
        void tardiaQueRechaza() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.RECHAZAR, "Mal", POLITICA, T1);

            ResultadoDeEvaluacion tardia = caso.evaluar("r2", Decision.RECHAZAR, "Yo también", POLITICA, T2);

            assertThat(tardia.decision()).isEqualTo(ResultadoDeDecision.PENDIENTE);
            assertThat(caso.rondaActual().decididaEn()).contains(T1);
        }

        @Test
        @DisplayName("rechazar exige una observación")
        void rechazarExigeObservacion() {
            conRevisores("r1");

            assertThatThrownBy(() -> caso.evaluar("r1", Decision.RECHAZAR, "  ", POLITICA, T1))
                    .isInstanceOf(RevisionInvalidaException.class).hasMessageContaining("observación");
            assertThatThrownBy(() -> caso.evaluar("r1", Decision.RECHAZAR, null, POLITICA, T1))
                    .isInstanceOf(RevisionInvalidaException.class);
            assertThat(caso.rondaActual().cantidadDeEvaluaciones()).isZero();
        }

        @Test
        @DisplayName("aprobar admite no escribir observación; una en blanco se guarda como ausente")
        void aprobarSinObservacion() {
            conRevisores("r1");

            ResultadoDeEvaluacion resultado = caso.evaluar("r1", Decision.APROBAR, "   ", POLITICA, T1);

            assertThat(resultado.evaluacion().observacion()).isNull();
        }

        @Test
        @DisplayName("la observación se recorta y tiene un largo máximo")
        void largoDeLaObservacion() {
            conRevisores("r1", "r2");

            ResultadoDeEvaluacion recortada = caso.evaluar("r1", Decision.APROBAR, "  Bien  ", POLITICA, T1);

            assertThat(recortada.evaluacion().observacion()).isEqualTo("Bien");
            String larga = "x".repeat(CasoDeRevision.MAXIMO_DE_OBSERVACION + 1);
            assertThatThrownBy(() -> caso.evaluar("r2", Decision.APROBAR, larga, POLITICA, T2))
                    .isInstanceOf(RevisionInvalidaException.class);
            assertThat(caso.evaluar("r2", Decision.APROBAR, "x".repeat(CasoDeRevision.MAXIMO_DE_OBSERVACION), POLITICA, T2)
                    .decidioLaRonda()).isTrue();
        }

        @Test
        @DisplayName("la decisión es obligatoria")
        void decisionObligatoria() {
            conRevisores("r1");

            assertThatThrownBy(() -> caso.evaluar("r1", null, "x", POLITICA, T1))
                    .isInstanceOf(RevisionInvalidaException.class);
        }

        @Test
        @DisplayName("solo evalúa quien está asignado a la ronda")
        void noAsignado() {
            conRevisores("r1");

            assertThatThrownBy(() -> caso.evaluar("intruso", Decision.APROBAR, null, POLITICA, T1))
                    .isInstanceOf(AccesoNoPermitidoException.class);
            assertThatThrownBy(() -> caso.evaluar(null, Decision.APROBAR, null, POLITICA, T1))
                    .isInstanceOf(AccesoNoPermitidoException.class);
        }

        @Test
        @DisplayName("la evaluación es definitiva: no se puede enviar dos veces")
        void definitiva() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.APROBAR, null, POLITICA, T1);

            assertThatThrownBy(() -> caso.evaluar("r1", Decision.RECHAZAR, "Cambié de opinión", POLITICA, T2))
                    .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("definitiva");
            assertThat(caso.rondaActual().evaluacionDe("r1").orElseThrow().decision()).isEqualTo(Decision.APROBAR);
        }

        @Test
        @DisplayName("no se evalúa una ronda que aún espera revisores, ya aprobada o cerrada")
        void estadosSinEvaluacion() {
            caso.iniciarRonda(1, T0);
            assertThatThrownBy(() -> caso.evaluar("r1", Decision.APROBAR, null, POLITICA, T1))
                    .isInstanceOf(AccesoNoPermitidoException.class); // aún no hay revisores

            caso.asignarRevisores(List.of("r1"), "admin", 5, T1);
            caso.evaluar("r1", Decision.APROBAR, null, POLITICA, T1);
            assertThatThrownBy(() -> caso.evaluar("r1", Decision.APROBAR, null, POLITICA, T2))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        @DisplayName("tras reabrir, la ronda cerrada ya no admite observaciones")
        void cerradaPorReapertura() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.RECHAZAR, "Mal", POLITICA, T1);

            assertThat(caso.cerrarRondaPorReapertura(T2)).isTrue();

            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.CERRADA);
            assertThatThrownBy(() -> caso.evaluar("r2", Decision.APROBAR, null, POLITICA, T2))
                    .isInstanceOf(OperacionNoPermitidaException.class).hasMessageContaining("no admite");
        }

        @Test
        @DisplayName("en la ronda nueva, los mismos revisores evalúan de nuevo")
        void rondaNuevaEvaluaDeNuevo() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.RECHAZAR, "Mal", POLITICA, T1);
            caso.iniciarRonda(2, T2);

            caso.evaluar("r1", Decision.APROBAR, null, POLITICA, T2);
            ResultadoDeEvaluacion fin = caso.evaluar("r2", Decision.APROBAR, null, POLITICA, T2);

            assertThat(fin.decision()).isEqualTo(ResultadoDeDecision.APROBADA);
            assertThat(caso.rondas()).hasSize(2);
            assertThat(caso.rondas().get(0).evaluaciones()).hasSize(1); // el historial de la ronda 1 se conserva
        }
    }

    @Nested
    @DisplayName("reapertura")
    class Reapertura {

        @Test
        @DisplayName("solo cierra una ronda rechazada")
        void soloRechazada() {
            assertThat(caso.cerrarRondaPorReapertura(T1)).isFalse(); // sin rondas
            conRevisores("r1");
            assertThat(caso.cerrarRondaPorReapertura(T1)).isFalse(); // en curso
            assertThat(caso.rondaActual().estado()).isEqualTo(EstadoRonda.EN_CURSO);
        }

        @Test
        @DisplayName("reabrir dos veces es inofensivo")
        void idempotente() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.RECHAZAR, "Mal", POLITICA, T1);

            assertThat(caso.cerrarRondaPorReapertura(T2)).isTrue();
            assertThat(caso.cerrarRondaPorReapertura(T2)).isFalse();
        }
    }

    @Nested
    @DisplayName("quién ve qué")
    class Vistas {

        private void rondaRechazadaConObservaciones() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.RECHAZAR, "Falta contexto", POLITICA, T1);
            caso.evaluar("r2", Decision.APROBAR, "Yo la veo bien", POLITICA, T2);
        }

        @Test
        @DisplayName("el administrador ve todas las rondas con revisores y evaluaciones")
        void administrador() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.APROBAR, "Bien", POLITICA, T1);

            VistaDelCaso vista = caso.vistaPara(new Observador("admin", true));

            VistaDelCaso.VistaDeRonda ronda = vista.rondas().get(0);
            assertThat(ronda.detalleVisible()).isTrue();
            assertThat(ronda.revisores()).hasSize(2);
            assertThat(ronda.revisores().get(0).evaluacion().observacion()).isEqualTo("Bien");
            assertThat(ronda.revisores().get(1).evaluacion()).isNull();
        }

        @Test
        @DisplayName("un revisor ve las observaciones de sus colegas, incluso con la ronda en curso")
        void revisorVeASusColegas() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.APROBAR, "Bien", POLITICA, T1);

            VistaDelCaso vista = caso.vistaPara(new Observador("r2", false));

            assertThat(vista.rondas().get(0).detalleVisible()).isTrue();
            assertThat(vista.rondas().get(0).revisores().get(0).evaluacion().observacion()).isEqualTo("Bien");
        }

        @Test
        @DisplayName("el autor solo ve el detalle de una ronda cuando ya está decidida")
        void autorSoloVeLoDecidido() {
            conRevisores("r1", "r2");
            caso.evaluar("r1", Decision.APROBAR, "Bien", POLITICA, T1);

            VistaDelCaso.VistaDeRonda enCurso = caso.vistaPara(new Observador("autor", false)).rondas().get(0);

            assertThat(enCurso.detalleVisible()).isFalse();
            assertThat(enCurso.revisores()).isEmpty();
            assertThat(enCurso.totalDeRevisores()).isEqualTo(2);
            assertThat(enCurso.totalDeEvaluadas()).isEqualTo(1);
        }

        @Test
        @DisplayName("el autor ve todas las observaciones de la ronda rechazada, también las tardías")
        void autorVeLaRondaRechazada() {
            rondaRechazadaConObservaciones();

            VistaDelCaso.VistaDeRonda ronda = caso.vistaPara(new Observador("autor", false)).rondas().get(0);

            assertThat(ronda.detalleVisible()).isTrue();
            assertThat(ronda.revisores()).extracting(r -> r.evaluacion().observacion())
                    .containsExactly("Falta contexto", "Yo la veo bien");
            assertThat(ronda.revisores().get(1).evaluacion().tardia()).isTrue();
        }

        @Test
        @DisplayName("un exrevisor sigue viendo la revisión aunque una ronda posterior sea de otros")
        void exRevisor() {
            conRevisores("r1");
            caso.evaluar("r1", Decision.RECHAZAR, "Mal", POLITICA, T1);

            assertThat(caso.participaComoRevisor("r1")).isTrue();
            assertThat(caso.vistaPara(new Observador("r1", false)).rondas()).hasSize(1);
        }

        @Test
        @DisplayName("quien no participa no puede ver la revisión")
        void ajeno() {
            conRevisores("r1");

            assertThatThrownBy(() -> caso.vistaPara(new Observador("otro", false)))
                    .isInstanceOf(AccesoNoPermitidoException.class);
            assertThat(caso.participaComoRevisor("otro")).isFalse();
        }

        @Test
        @DisplayName("la vista refleja el título, la competencia y el autor")
        void datosGenerales() {
            VistaDelCaso vista = caso.vistaPara(new Observador("admin", true));

            assertThat(vista.preguntaId()).isEqualTo("q-1");
            assertThat(vista.autorId()).isEqualTo("autor");
            assertThat(vista.titulo()).isEqualTo("Patrón Observer");
            assertThat(vista.competencia()).isEqualTo("LECTURA_CRITICA");
            assertThat(vista.rondas()).isEmpty();
        }
    }

    @Nested
    @DisplayName("construcción")
    class Construccion {

        @Test
        @DisplayName("exige identificador, pregunta y autor")
        void obligatorios() {
            assertThatThrownBy(() -> CasoDeRevision.nuevo(" ", "q", "a", "t", "c", T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CasoDeRevision.nuevo("c", null, "a", "t", "c", T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CasoDeRevision.nuevo("c", "q", "", "t", "c", T0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("un título o competencia ausentes quedan como texto vacío")
        void textosAusentes() {
            CasoDeRevision c = CasoDeRevision.nuevo("c", "q", "a", null, null, T0);

            assertThat(c.titulo()).isEmpty();
            assertThat(c.competencia()).isEmpty();
        }

        @Test
        @DisplayName("la lista de rondas no se puede modificar desde fuera")
        void rondasInmodificables() {
            caso.iniciarRonda(1, T0);

            assertThatThrownBy(() -> caso.rondas().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("pedir la ronda actual sin rondas es una operación no permitida")
        void sinRondaActual() {
            assertThat(caso.ultimaRonda()).isEmpty();
            assertThatThrownBy(() -> caso.rondaActual()).isInstanceOf(OperacionNoPermitidaException.class);
        }
    }
}
