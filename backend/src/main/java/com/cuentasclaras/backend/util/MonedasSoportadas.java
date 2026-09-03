package com.cuentasclaras.backend.util;

import java.util.Set;

/**
 * Símbolos de moneda soportados para la conversión a USDT vía CriptoYa
 * (Binance P2P). {@code USDT} está en {@link #CRIPTOS} pero se trata como caso
 * especial (sin llamada externa, tasa 1).
 */
public final class MonedasSoportadas {

    private MonedasSoportadas() {
    }

    public static final Set<String> FIATS = Set.of(
            "ARS", "BRL", "CLP", "COP", "MXN", "PEN", "VES", "BOB", "UYU", "DOP",
            "PYG", "USD", "EUR");

    public static final Set<String> CRIPTOS = Set.of(
            "USDT", "BTC", "ETH", "USDC", "DAI", "UXD", "USDP", "WLD", "BNB", "SOL",
            "XRP", "ADA", "AVAX", "DOGE", "TRX", "LINK", "DOT", "MATIC", "SHIB", "LTC",
            "BCH", "EOS", "XLM", "FTM", "AAVE", "UNI", "ALGO", "BAT", "PAXG", "CAKE",
            "AXS", "SLP", "MANA", "SAND", "CHZ");

    public static boolean esUsdt(String moneda) {
        return "USDT".equals(moneda);
    }

    public static boolean esFiat(String moneda) {
        return FIATS.contains(moneda);
    }

    public static boolean esCripto(String moneda) {
        return CRIPTOS.contains(moneda);
    }

    public static boolean esSoportada(String moneda) {
        return FIATS.contains(moneda) || CRIPTOS.contains(moneda);
    }
}
