package com.cuentasclaras.backend.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegistrarGastoRequest(
        @NotBlank(message = "La descripción es obligatoria") @Size(max = 255, message = "La descripción no puede superar los 255 caracteres") String descripcion,
        @NotNull(message = "El monto es obligatorio") @Positive(message = "El monto debe ser mayor que 0") @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 8 enteros y 2 decimales") BigDecimal monto,
        @NotNull(message = "El pagadorId es obligatorio") Long pagadorId,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha) {
}
