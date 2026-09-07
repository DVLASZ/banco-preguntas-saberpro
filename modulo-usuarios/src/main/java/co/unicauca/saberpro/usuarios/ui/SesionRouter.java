package co.unicauca.saberpro.usuarios.ui;

import co.unicauca.saberpro.usuarios.domain.User;

/**
 * Abstraccion que decide que ventana abrir tras un login exitoso, segun el
 * rol del usuario autenticado. {@link LoginFrame} depende unicamente de
 * esta interfaz — nunca de las ventanas concretas del modulo de preguntas
 * (Autor, Revisor, etc.) — para no romper la Inversion de Dependencias:
 * quien arma la implementacion real (y por lo tanto conoce ambos modulos,
 * usuarios y preguntas) es el composition root de la aplicacion
 * ({@code app.MainApp}).
 */
public interface SesionRouter {

    void abrirVistaPara(User user);
}
