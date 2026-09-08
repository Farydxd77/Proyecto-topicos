package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;

/**
 * Lo que le toca a un participante en un gasto. El {@code peso} son las partes que
 * se le aplicaron al repartir: {@code 1} para todos en un reparto equitativo. Se
 * devuelve para que el cliente pueda reconstruir cómo se dividió el gasto sin
 * deducirlo de los montos.
 */
public record GastoParticipanteDto(
        ParticipanteDto participante,
        BigDecimal montoAdeudado,
        Integer peso) {
}
