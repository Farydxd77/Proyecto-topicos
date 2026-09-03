package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;

public record TransferenciaDto(
        String de,
        Long deId,
        String para,
        Long paraId,
        BigDecimal monto) {
}
