package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GastoResponse(
        Long id,
        Long grupoId,
        String descripcion,
        BigDecimal monto,
        String moneda,
        String monedaNombre,
        BigDecimal montoUsdt,
        BigDecimal tasaCambio,
        ParticipanteDto pagador,
        LocalDate fecha,
        List<GastoParticipanteDto> division) {
}
