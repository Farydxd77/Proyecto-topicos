package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;

/**
 * Balance neto de un participante en un grupo.
 *
 * <p>{@code esMiembroActual} distingue a un integrante del grupo de alguien que
 * aparece solo por su actividad pasada (gastos o pagos) tras salir. No influye en el
 * cálculo del balance ni en quién entra en la respuesta: los ex-miembros con saldo
 * tienen que seguir apareciendo para que la suma de todos los balances dé cero.
 */
public record BalanceDto(
        ParticipanteDto participante,
        BigDecimal balance,
        boolean esMiembroActual) {
}
