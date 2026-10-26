package co.unicauca.bancopreguntas.audit.application.port;

import co.unicauca.bancopreguntas.audit.domain.EntradaDeAuditoria;
import co.unicauca.bancopreguntas.audit.domain.FiltroAuditoria;
import co.unicauca.bancopreguntas.audit.domain.Pagina;

/** Puerto de salida: persistencia de la auditoría. Solo se agrega, nunca se modifica ni se borra. */
public interface AuditRepository {

    void guardar(EntradaDeAuditoria entrada);

    /** Toda la actividad registrada (incluida la de seguridad), con filtros opcionales, lo más reciente primero. */
    Pagina<EntradaDeAuditoria> consultar(FiltroAuditoria filtro, int pagina, int tamano);
}
