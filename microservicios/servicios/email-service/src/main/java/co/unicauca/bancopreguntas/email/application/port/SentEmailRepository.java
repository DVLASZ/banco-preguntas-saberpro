package co.unicauca.bancopreguntas.email.application.port;

import co.unicauca.bancopreguntas.email.domain.CorreoEnviado;
import co.unicauca.bancopreguntas.email.domain.EstadoCorreo;
import co.unicauca.bancopreguntas.email.domain.Pagina;

/** Puerto de salida: registro de los correos enviados. */
public interface SentEmailRepository {

    void guardar(CorreoEnviado correo);

    /** Los correos, el más reciente primero; {@code estado} {@code null} trae todos. */
    Pagina<CorreoEnviado> listar(EstadoCorreo estado, int pagina, int tamano);
}
