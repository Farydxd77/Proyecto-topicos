package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoResponse(
        Long id,
        Long grupoId,
        ParticipanteDto pagador,
        ParticipanteDto receptor,
        BigDecimal monto,
        LocalDate fecha,
        String txId) {
}
