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
     * Balance neto de cada participante considerando solo los gastos del grupo.
     * Equivale a {@link #calcularBalances(Set, Map, Map, Map, Map)} sin pagos.
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

        return calcularBalances(participantesIds, pagadoPorId, adeudadoPorId, Map.of(), Map.of());
    }

    /**
     * Balance neto de cada participante: lo que pagó en gastos menos lo que le
     * corresponde adeudar, más lo que pagó a otros y menos lo que recibió en
     * pagos. Un pago del deudor al acreedor acerca ambos balances a cero. La
     * suma de todos los balances es exactamente {@code 0.00} siempre que, para
     * cada gasto, la suma de lo adeudado iguale su monto (invariante garantizado
     * por la capacidad de gastos) y cada pago mueva su monto exacto entre dos
     * participantes.
     *
     * @param participantesIds       todos los participantes a incluir (miembros del
     *                                grupo y cualquiera con actividad en un gasto o pago)
     * @param pagadoPorId            suma de los montos en USDT de los gastos que pagó cada id
     * @param adeudadoPorId          suma de los {@code monto_adeudado} de cada id
     * @param pagosRealizadosPorId   suma de los pagos que realizó cada id
     * @param pagosRecibidosPorId    suma de los pagos que recibió cada id
     * @return mapa id → balance con escala 2, ordenado por id ascendente
     */
    public static Map<Long, BigDecimal> calcularBalances(
            Set<Long> participantesIds,
            Map<Long, BigDecimal> pagadoPorId,
            Map<Long, BigDecimal> adeudadoPorId,
            Map<Long, BigDecimal> pagosRealizadosPorId,
            Map<Long, BigDecimal> pagosRecibidosPorId) {

        return calcularBalances(participantesIds, pagadoPorId, adeudadoPorId,
                pagosRealizadosPorId, pagosRecibidosPorId, Map.of());
    }

    /**
     * Balance neto incluyendo el efecto de las bajas que el grupo asumió.
     *
     * <p>El ajuste de una baja es un único número con signo por participante: lo que
     * esa baja le suma o le resta a su balance. Quien se fue recibe {@code -saldo},
     * que lo lleva exactamente a cero; cada quien lo absorbe recibe su parte de ese
     * saldo. Como las partes suman el saldo, el ajuste de una baja suma cero entre
     * todos, y por eso la suma de todos los balances sigue dando exactamente cero.
     *
     * <p>Un solo número con signo cubre las dos direcciones. Si quien se fue debía
     * 300, recibe {@code +300} y los demás {@code -150} cada uno: sus balances bajan.
     * Si le debían 300 a él, recibe {@code -300} y los demás {@code +150}: sus
     * balances suben, porque dejan de deberle.
     *
     * @param ajusteBajasPorId lo que las bajas asumidas le suman (o restan) a cada
     *                         participante
     */
    public static Map<Long, BigDecimal> calcularBalances(
            Set<Long> participantesIds,
            Map<Long, BigDecimal> pagadoPorId,
            Map<Long, BigDecimal> adeudadoPorId,
            Map<Long, BigDecimal> pagosRealizadosPorId,
            Map<Long, BigDecimal> pagosRecibidosPorId,
            Map<Long, BigDecimal> ajusteBajasPorId) {

        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        participantesIds.stream().sorted().forEach(id -> {
            BigDecimal pagado = pagadoPorId.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal adeudado = adeudadoPorId.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal pagosRealizados = pagosRealizadosPorId.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal pagosRecibidos = pagosRecibidosPorId.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal ajusteBajas = ajusteBajasPorId.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal balance = pagado.subtract(adeudado)
                    .add(pagosRealizados).subtract(pagosRecibidos)
                    .add(ajusteBajas);
            balances.put(id, balance.setScale(2, RoundingMode.HALF_UP));
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
