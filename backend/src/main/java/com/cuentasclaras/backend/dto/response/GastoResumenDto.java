package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoResumenDto(
        Long id,
        String descripcion,
        BigDecimal monto,
        ParticipanteDto pagador,
        LocalDate fecha) {
}
