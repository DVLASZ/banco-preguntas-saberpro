package co.unicauca.bancopreguntas.user.application;

/** Lo que cambió al sincronizar los usuarios con el proveedor de identidad. */
public record ResultadoDeSincronizacion(int creados, int actualizados, int desactivados) {
}
