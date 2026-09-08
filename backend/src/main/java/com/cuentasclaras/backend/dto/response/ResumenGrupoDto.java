package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;

/**
 * Totales agregados de un grupo, todos en USDT con 2 decimales.
 *
 * <p><b>{@code totalPagado} no converge a {@code totalGastado}, y no debe hacerlo.</b>
 * Quien paga un gasto cubre su propia parte en ese mismo momento y nunca se
 * transfiere dinero a sí mismo, así que esa porción jamás aparece como pago. En un
 * grupo de 3 donde Ana paga 900 repartidos en partes iguales, la deuda hacia Ana es
 * 600: cuando los otros dos le transfieran sus 300, el grupo queda saldado con
 * {@code totalPagado = 600} y {@code totalGastado = 900}.
 *
 * <p>La relación que sí cierra, y la que mide el avance de la liquidación, es
 * {@code deudaTotalDelGrupo = totalPagado + pendientePorSaldar}.
 *
 * @param totalGastado       suma de los montos en USDT de todos los gastos
 * @param totalPagado        suma de los montos de todos los pagos registrados
 * @param pendientePorSaldar suma de los balances positivos: lo que falta transferir
 *                           para que todos queden a mano
 * @param miParte            lo que le corresponde adeudar a quien consulta, sumando
 *                           su parte en todos los gastos
 */
public record ResumenGrupoDto(
        BigDecimal totalGastado,
        BigDecimal totalPagado,
        BigDecimal pendientePorSaldar,
        BigDecimal miParte,
        int cantidadGastos,
        int cantidadPagos) {
}
