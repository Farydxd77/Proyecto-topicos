package com.cuentasclaras.backend.entity;

/**
 * Estado de la decisión del grupo sobre la deuda de quien se fue.
 *
 * <p>Son tres y no un booleano porque «todavía no lo decidieron» y «decidieron que
 * no» son cosas distintas: la primera es una tarea pendiente del creador, la segunda
 * un tema cerrado que quedó anotado.
 */
public enum EstadoBaja {

    /** Salió con saldo y el creador todavía no decidió. No altera ningún balance. */
    PENDIENTE,

    /** El grupo se hizo cargo: el saldo se repartió entre los miembros actuales. */
    ASUMIDA,

    /** El grupo no se hizo cargo: el saldo queda como un tema abierto, sin tocar. */
    NO_ASUMIDA
}
