package co.unicauca.saberpro.usuarios.domain.menu;

import co.unicauca.saberpro.usuarios.domain.Role;

import java.util.List;

/**
 * Estrategia que entrega las opciones de menu/tablero correspondientes a un
 * rol. Cada rol tiene su propia implementacion; agregar un nuevo rol solo
 * requiere una nueva clase, sin modificar las existentes ni el codigo que
 * las usa (Abierto/Cerrado). Cualquier implementacion puede sustituir a
 * otra sin romper el contrato (Sustitucion de Liskov).
 */
public interface IMenuProvider {

    Role getRole();

    List<String> getMenuOptions();
}
