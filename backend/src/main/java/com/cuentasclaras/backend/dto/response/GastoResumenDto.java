package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoResumenDto(
        Long id,
        String descripcion,
        BigDecimal monto,
        String moneda,
        String monedaNombre,
        BigDecimal montoUsdt,
        ParticipanteDto pagador,
        LocalDate fecha) {
}
