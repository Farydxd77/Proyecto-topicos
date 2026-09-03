package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;

public record GastoParticipanteDto(
        ParticipanteDto participante,
        BigDecimal montoAdeudado) {
}
