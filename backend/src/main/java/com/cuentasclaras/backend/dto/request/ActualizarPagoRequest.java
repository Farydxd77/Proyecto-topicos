package com.cuentasclaras.backend.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Edición de un pago. El grupo y el pagador del pago no cambian; solo se
 * actualizan receptor, monto, fecha y txId.
 */
public record ActualizarPagoRequest(
        @NotNull(message = "El receptorId es obligatorio") Long receptorId,
        @NotNull(message = "El monto es obligatorio") @Positive(message = "El monto debe ser mayor que 0") @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 10 enteros y 2 decimales") BigDecimal monto,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @Size(max = 100, message = "El txId admite hasta 100 caracteres") String txId) {
}
