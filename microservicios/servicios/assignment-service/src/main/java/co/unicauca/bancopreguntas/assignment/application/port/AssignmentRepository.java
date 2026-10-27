package co.unicauca.bancopreguntas.assignment.application.port;

import co.unicauca.bancopreguntas.assignment.domain.AsignacionDeRevisor;
import co.unicauca.bancopreguntas.assignment.domain.CasoDeAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.CasoPendiente;
import co.unicauca.bancopreguntas.assignment.domain.FiltroAsignacion;
import co.unicauca.bancopreguntas.assignment.domain.Pagina;

import java.util.Optional;

/**
 * Puerto de salida: persistencia de las asignaciones. El dominio y la aplicación dependen de esta abstracción;
 * el adaptador JPA la implementa.
 */
public interface AssignmentRepository {

    /** Crea o actualiza el caso completo (rondas y asignaciones). */
    void guardar(CasoDeAsignacion caso);

    /** Busca el caso de una pregunta, solo para consultarlo. */
    Optional<CasoDeAsignacion> buscarPorPreguntaId(String preguntaId);

    /**
     * Busca el caso de una pregunta para modificarlo y bloquea su registro hasta el fin de la transacción:
     * así dos cambios simultáneos no se pisan con datos viejos.
     */
    Optional<CasoDeAsignacion> buscarPorPreguntaIdParaActualizar(String preguntaId);

    /** Las preguntas cuya ronda espera que el administrador asigne revisores, las más antiguas primero. */
    Pagina<CasoPendiente> pendientesDeAsignacion(int pagina, int tamano);

    /** La bandeja de un revisor, las asignaciones más recientes primero. */
    Pagina<AsignacionDeRevisor> asignadasA(String revisorId, FiltroAsignacion filtro, int pagina, int tamano);
}
