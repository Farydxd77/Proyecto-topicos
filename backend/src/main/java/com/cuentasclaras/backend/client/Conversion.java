package com.cuentasclaras.backend.client;

import java.math.BigDecimal;

/** Resultado de convertir un monto a USDT: {@code montoUsdt = montoOriginal * tasaCambio}. */
public record Conversion(BigDecimal montoUsdt, BigDecimal tasaCambio) {
}
