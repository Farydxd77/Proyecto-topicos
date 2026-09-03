package com.cuentasclaras.backend.balances;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cuentasclaras.backend.util.BalanceUtil;
import com.cuentasclaras.backend.util.BalanceUtil.Movimiento;

class BalanceUtilTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal suma(Map<Long, BigDecimal> balances) {
        return balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Map<Long, BigDecimal> balancesDe(Object... pares) {
        Map<Long, BigDecimal> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put(((Number) pares[i]).longValue(), bd((String) pares[i + 1]));
        }
        return m;
    }

    // 5.1 calcularBalances --------------------------------------------------

    @Test
    void calcularBalances_pagadorPositivoDeudorNegativoSinActividadCero() {
        Map<Long, BigDecimal> pagado = new HashMap<>(Map.of(1L, bd("90.00")));
        Map<Long, BigDecimal> adeudado = new HashMap<>(Map.of(
                1L, bd("30.00"), 2L, bd("30.00"), 3L, bd("30.00")));

        Map<Long, BigDecimal> balances = BalanceUtil.calcularBalances(
                Set.of(1L, 2L, 3L, 4L), pagado, adeudado);

        assertThat(balances.get(1L)).isEqualByComparingTo("60.00");   // solo pagó de más
        assertThat(balances.get(2L)).isEqualByComparingTo("-30.00");  // solo adeuda
        assertThat(balances.get(3L)).isEqualByComparingTo("-30.00");
        assertThat(balances.get(4L)).isEqualByComparingTo("0.00");    // sin actividad
        assertThat(suma(balances)).isEqualByComparingTo("0.00");
    }

    @Test
    void calcularBalances_divisionNoExacta_sumaExactamenteCero() {
        // Lo que produce la capacidad de gastos para 100.00 entre 3 (pagador id 1).
        Map<Long, BigDecimal> pagado = new HashMap<>(Map.of(1L, bd("100.00")));
        Map<Long, BigDecimal> adeudado = new HashMap<>(Map.of(
                1L, bd("33.34"), 2L, bd("33.33"), 3L, bd("33.33")));

        Map<Long, BigDecimal> balances = BalanceUtil.calcularBalances(
                Set.of(1L, 2L, 3L), pagado, adeudado);

        assertThat(balances.get(1L)).isEqualByComparingTo("66.66");
        assertThat(balances.get(2L)).isEqualByComparingTo("-33.33");
        assertThat(balances.get(3L)).isEqualByComparingTo("-33.33");
        assertThat(suma(balances)).isEqualByComparingTo("0.00");
        assertThat(balances.get(1L).scale()).isEqualTo(2);
    }

    /**
     * Un único gasto de {@code monto} pagado por el participante 1 y repartido
     * entre {1, 2, 3} con la misma regla que {@code GastoService.calcularDivision}
     * (HALF_UP a 2 decimales, el pagador absorbe el residuo). Verifica que la
     * división cuadra con el monto y que la suma de los balances es exactamente 0.
     */
    private void verificarGastoNoExactoEntre3(
            String monto, String porPersonaEsperado, String montoPagadorEsperado) {

        BigDecimal m = bd(monto);
        BigDecimal porPersona = m.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal montoPagador = m.subtract(porPersona.multiply(BigDecimal.valueOf(2)));

        assertThat(porPersona).isEqualByComparingTo(porPersonaEsperado);
        assertThat(montoPagador).isEqualByComparingTo(montoPagadorEsperado);
        // La suma de la división del gasto iguala exactamente el monto.
        assertThat(porPersona.add(porPersona).add(montoPagador)).isEqualByComparingTo(monto);

        Map<Long, BigDecimal> pagado = new HashMap<>(Map.of(1L, m));
        Map<Long, BigDecimal> adeudado = new HashMap<>(Map.of(
                1L, montoPagador, 2L, porPersona, 3L, porPersona));

        Map<Long, BigDecimal> balances = BalanceUtil.calcularBalances(
                Set.of(1L, 2L, 3L), pagado, adeudado);

        assertThat(balances.get(1L)).isEqualByComparingTo(m.subtract(montoPagador));
        assertThat(balances.get(2L)).isEqualByComparingTo(porPersona.negate());
        assertThat(balances.get(3L)).isEqualByComparingTo(porPersona.negate());
        assertThat(suma(balances)).isEqualByComparingTo("0.00");
    }

    @Test
    void calcularBalances_50entre3_divisionYSumaCuadran() {
        verificarGastoNoExactoEntre3("50.00", "16.67", "16.66");
    }

    @Test
    void calcularBalances_10entre3_divisionYSumaCuadran() {
        verificarGastoNoExactoEntre3("10.00", "3.33", "3.34");
    }

    @Test
    void calcularBalances_1entre3_divisionYSumaCuadran() {
        verificarGastoNoExactoEntre3("1.00", "0.33", "0.34");
    }

    @Test
    void calcularBalances_ordenadoPorIdAscendente() {
        Map<Long, BigDecimal> balances = BalanceUtil.calcularBalances(
                Set.of(30L, 10L, 20L), new HashMap<>(), new HashMap<>());

        assertThat(balances.keySet()).containsExactly(10L, 20L, 30L);
    }

    // 5.2 minimizarTransferencias ----------------------------------------

    @Test
    void minimizar_escenarioSamaipata_tresTransferenciasDe200HaciaAna() {
        List<Movimiento> movs = BalanceUtil.minimizarTransferencias(
                balancesDe(1, "600.00", 2, "-200.00", 3, "-200.00", 4, "-200.00"));

        assertThat(movs).hasSize(3);
        assertThat(movs).allSatisfy(m -> {
            assertThat(m.paraId()).isEqualTo(1L);
            assertThat(m.monto()).isEqualByComparingTo("200.00");
        });
        assertThat(movs).extracting(Movimiento::deId).containsExactlyInAnyOrder(2L, 3L, 4L);
    }

    @Test
    void minimizar_unDeudorCubreAVariosAcreedores() {
        List<Movimiento> movs = BalanceUtil.minimizarTransferencias(
                balancesDe(1, "-300.00", 2, "200.00", 3, "100.00"));

        assertThat(movs).hasSize(2);
        assertThat(movs.get(0)).isEqualTo(new Movimiento(1L, 2L, bd("200.00")));
        assertThat(movs.get(1)).isEqualTo(new Movimiento(1L, 3L, bd("100.00")));
    }

    @Test
    void minimizar_unAcreedorRecibeDeVariosDeudores() {
        List<Movimiento> movs = BalanceUtil.minimizarTransferencias(
                balancesDe(1, "300.00", 2, "-100.00", 3, "-200.00"));

        assertThat(movs).hasSize(2);
        assertThat(movs.get(0)).isEqualTo(new Movimiento(3L, 1L, bd("200.00")));
        assertThat(movs.get(1)).isEqualTo(new Movimiento(2L, 1L, bd("100.00")));
    }

    @Test
    void minimizar_todosEnCero_listaVacia() {
        assertThat(BalanceUtil.minimizarTransferencias(
                balancesDe(1, "0.00", 2, "0.00", 3, "0.00"))).isEmpty();
    }

    @Test
    void minimizar_nuncaEmiteMontoCero_yLasSumasCuadran() {
        Map<Long, BigDecimal> balances = balancesDe(
                1, "150.00", 2, "50.00", 3, "-70.00", 4, "-130.00");

        List<Movimiento> movs = BalanceUtil.minimizarTransferencias(balances);

        assertThat(movs).isNotEmpty();
        assertThat(movs).allSatisfy(m -> assertThat(m.monto()).isGreaterThan(BigDecimal.ZERO));

        Map<Long, BigDecimal> neto = new HashMap<>();
        for (Movimiento m : movs) {
            neto.merge(m.deId(), m.monto().negate(), BigDecimal::add);
            neto.merge(m.paraId(), m.monto(), BigDecimal::add);
        }
        balances.forEach((id, saldo) ->
                assertThat(neto.getOrDefault(id, BigDecimal.ZERO)).isEqualByComparingTo(saldo));
    }
}
