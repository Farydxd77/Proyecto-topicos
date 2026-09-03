package com.cuentasclaras.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record AgregarMiembroRequest(
        @NotNull(message = "El participanteId es obligatorio") Long participanteId) {
}
