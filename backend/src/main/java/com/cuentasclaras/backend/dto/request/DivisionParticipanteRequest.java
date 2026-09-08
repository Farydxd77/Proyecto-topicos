package com.cuentasclaras.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Una entrada de la división explícita de un gasto: quién participa y con cuántas
 * partes. Excluir a alguien es no incluirlo en la lista; repartir desigual es darle
 * un peso distinto. Un reparto equitativo entre los listados es todos con peso 1.
 */
public record DivisionParticipanteRequest(
        @NotNull(message = "El participanteId es obligatorio") Long participanteId,
        @NotNull(message = "El peso es obligatorio") @Min(value = 1, message = "El peso debe ser al menos 1") @Max(value = 1000, message = "El peso no puede superar 1000") Integer peso) {
}
