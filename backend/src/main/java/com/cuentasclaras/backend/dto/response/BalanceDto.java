package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;

public record BalanceDto(
        ParticipanteDto participante,
        BigDecimal balance) {
}
