package com.cuentasclaras.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Transferencia del rol de creador de un grupo. El destinatario debe ser un miembro
 * actual del grupo distinto del creador que la solicita.
 */
public record TransferirCreadorRequest(
        @NotNull(message = "El participanteId es obligatorio") Long participanteId) {
}
