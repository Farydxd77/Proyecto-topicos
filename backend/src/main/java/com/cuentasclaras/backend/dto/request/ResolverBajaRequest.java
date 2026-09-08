package com.cuentasclaras.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * La decisión del grupo sobre la deuda de quien se fue.
 *
 * <p>{@code true}: el grupo se hace cargo y el saldo se reparte en partes iguales
 * entre los miembros actuales. {@code false}: no se hace cargo y el saldo queda como
 * un tema abierto, sin alterar ningún balance.
 */
public record ResolverBajaRequest(
        @NotNull(message = "Hay que indicar si el grupo asume la deuda") Boolean asumir) {
}
