package com.cuentasclaras.backend.client;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.cuentasclaras.backend.exception.ServicioExternoNoDisponibleException;

/**
 * Cliente HTTP de la API pública de CriptoYa (Binance P2P) para convertir montos
 * a USDT. Cualquier fallo de la API externa se traduce a
 * {@link ServicioExternoNoDisponibleException} (→ HTTP 503).
 */
@Component
public class CriptoYaClient {

    private static final int ESCALA = 6;

    private final RestClient restClient;

    public CriptoYaClient(RestClient.Builder builder,
            @Value("${criptoya.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /** Precio P2P de un par: {@code {"ask": 7.10, "bid": 6.85, "time": 1234567890}}. */
    public record PrecioP2P(BigDecimal ask, BigDecimal bid, Long time) {
    }

    /**
     * Convierte un monto en moneda fiat a USDT: consulta {@code USDT/{moneda}},
     * {@code tasa = 1 / bid}, {@code montoUsdt = montoOriginal * tasa}.
     */
    public Conversion convertirFiatAUsdt(String moneda, BigDecimal montoOriginal) {
        BigDecimal bid = precio("/USDT/" + moneda + "/1").bid();
        BigDecimal tasa = BigDecimal.ONE.divide(bid, ESCALA, RoundingMode.HALF_UP);
        return new Conversion(aUsdt(montoOriginal, tasa), tasa);
    }

    /**
     * Convierte un monto en criptomoneda a USDT: consulta {@code {moneda}/USD} y
     * {@code USDT/USD}, {@code tasa = bidMoneda / bidUsdt},
     * {@code montoUsdt = montoOriginal * tasa}.
     */
    public Conversion convertirCriptoAUsdt(String moneda, BigDecimal montoOriginal) {
        BigDecimal bidMoneda = precio("/" + moneda + "/USD/1").bid();
        BigDecimal bidUsdt = precio("/USDT/USD/1").bid();
        BigDecimal tasa = bidMoneda.divide(bidUsdt, ESCALA, RoundingMode.HALF_UP);
        return new Conversion(aUsdt(montoOriginal, tasa), tasa);
    }

    private BigDecimal aUsdt(BigDecimal montoOriginal, BigDecimal tasa) {
        return montoOriginal.multiply(tasa).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private PrecioP2P precio(String path) {
        PrecioP2P precio;
        try {
            precio = restClient.get().uri(path).retrieve().body(PrecioP2P.class);
        } catch (RestClientException ex) {
            throw new ServicioExternoNoDisponibleException(
                    "CriptoYa no está disponible", ex);
        }
        if (precio == null || precio.bid() == null || precio.bid().signum() <= 0) {
            throw new ServicioExternoNoDisponibleException(
                    "CriptoYa devolvió una respuesta inválida para " + path);
        }
        return precio;
    }
}
