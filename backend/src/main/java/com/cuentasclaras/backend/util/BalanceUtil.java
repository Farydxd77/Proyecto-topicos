package com.cuentasclaras.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lógica pura de balances y liquidación de un grupo. Sin dependencias de Spring
 * ni de JPA: opera solo sobre identificadores y {@link BigDecimal}.
 */
public final class BalanceUtil {

    private BalanceUtil() {
    }

    /**
     * Una transferencia calculada: el participante {@code deId} le paga
     * {@code monto} al participante {@code paraId}.
     */
    public record Movimiento(Long deId, Long paraId, BigDecimal monto) {
    }

    /**
     * Balance neto de cada participante: lo que pagó menos lo que le corresponde
     * adeudar. La suma de todos los balances es exactamente {@code 0.00} siempre
     * que, para cada gasto, la suma de lo adeudado iguale su monto (invariante
     * garantizado por la capacidad de gastos).
     *
     * @param participantesIds todos los participantes a incluir (miembros del
     *                          grupo y cualquiera con actividad en un gasto)
     * @param pagadoPorId       suma de los montos de los gastos que pagó cada id
     * @param adeudadoPorId     suma de los {@code monto_adeudado} de cada id
     * @return mapa id → balance con escala 2, ordenado por id ascendente
     */
    public static Map<Long, BigDecimal> calcularBalances(
            Set<Long> participantesIds,
            Map<Long, BigDecimal> pagadoPorId,
            Map<Long, BigDecimal> adeudadoPorId) {

        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        participantesIds.stream().sorted().forEach(id -> {
            BigDecimal pagado = pagadoPorId.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal adeudado = adeudadoPorId.getOrDefault(id, BigDecimal.ZERO);
            balances.put(id, pagado.subtract(adeudado).setScale(2, RoundingMode.HALF_UP));
        });
        return balances;
    }

    /**
     * Algoritmo greedy de mínimas transferencias: el mayor deudor le paga al
     * mayor acreedor lo que pueda, y se repite hasta que todos los balances
     * quedan en cero. Los participantes con balance cero no aparecen. Nunca se
     * emite un movimiento de monto cero.
     *
     * @param balances mapa id → balance (positivo = acreedor, negativo = deudor)
     * @return lista de movimientos en el orden en que se generaron
     */
    public static List<Movimiento> minimizarTransferencias(Map<Long, BigDecimal> balances) {
        Comparator<Saldo> porMagnitudYId = Comparator
                .comparing((Saldo s) -> s.saldo.abs()).reversed()
                .thenComparing(s -> s.id);

        List<Saldo> acreedores = new ArrayList<>();
        List<Saldo> deudores = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : balances.entrySet()) {
            int signo = e.getValue().signum();
            if (signo > 0) {
                acreedores.add(new Saldo(e.getKey(), e.getValue()));
            } else if (signo < 0) {
                deudores.add(new Saldo(e.getKey(), e.getValue()));
            }
        }
        acreedores.sort(porMagnitudYId);
        deudores.sort(porMagnitudYId);

        List<Movimiento> movimientos = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < deudores.size() && j < acreedores.size()) {
            Saldo deudor = deudores.get(i);
            Saldo acreedor = acreedores.get(j);

            BigDecimal monto = deudor.saldo.negate().min(acreedor.saldo);
            if (monto.signum() > 0) {
                movimientos.add(new Movimiento(deudor.id, acreedor.id, monto));
                deudor.saldo = deudor.saldo.add(monto);
                acreedor.saldo = acreedor.saldo.subtract(monto);
            }

            if (deudor.saldo.signum() == 0) {
                i++;
            }
            if (acreedor.saldo.signum() == 0) {
                j++;
            }
        }
        return movimientos;
    }

    private static final class Saldo {
        private final Long id;
        private BigDecimal saldo;

        private Saldo(Long id, BigDecimal saldo) {
            this.id = id;
            this.saldo = saldo;
        }
    }
}
