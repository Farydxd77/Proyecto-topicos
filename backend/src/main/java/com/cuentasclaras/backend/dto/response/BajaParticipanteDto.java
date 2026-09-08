package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;

/**
 * Cuánto absorbió un participante de una baja asumida. El {@code monto} puede ser
 * negativo cuando quien se fue tenía saldo a favor: absorber es entonces dejar de
 * deberle, y el balance de quien absorbe sube.
 */
public record BajaParticipanteDto(
        ParticipanteDto participante,
        BigDecimal monto) {
}
