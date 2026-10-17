package co.unicauca.bancopreguntas.user.api.dto;

import co.unicauca.bancopreguntas.user.application.ResultadoDeSincronizacion;

/** Resultado de sincronizar a pedido: cambios en los usuarios y cuántos inicios de sesión se publicaron. */
public record SyncResponse(int usuariosCreados, int usuariosActualizados, int usuariosDesactivados,
                           int iniciosDeSesionPublicados) {

    public static SyncResponse de(ResultadoDeSincronizacion usuarios, int sesiones) {
        return new SyncResponse(usuarios.creados(), usuarios.actualizados(), usuarios.desactivados(), sesiones);
    }
}
