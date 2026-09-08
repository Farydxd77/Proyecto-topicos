package com.cuentasclaras.backend.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.cuentasclaras.backend.exception.ServicioExternoNoDisponibleException;

/**
 * Cliente HTTP de la API pública de CriptoYa (Binance P2P) para convertir montos
 * a USDT.
 *
 * <p>Cachea en memoria el {@code bid} de cada par con dos ventanas de tiempo
 * distintas, y la diferencia entre ellas es deliberada:
 *
 * <ul>
 * <li><b>TTL</b>: mientras la cotización es más nueva que esto, ni se pregunta.
 * Registrar cinco gastos seguidos en bolivianos hace una sola llamada externa.</li>
 * <li><b>Tolerancia máxima</b>: pasado el TTL se intenta la llamada real; si falla y
 * la cotización sigue dentro de esta ventana, se usa igual. Es la diferencia entre
 * «no hace falta preguntar» y «no se pudo preguntar y esto es lo mejor que tengo»:
 * sin ella, una caída de CriptoYa deja la aplicación inutilizable para cualquier
 * gasto que no sea en USDT.</li>
 * </ul>
 *
 * <p>Si no hay ninguna cotización previa del par, o la que hay superó la tolerancia,
 * se lanza {@link ServicioExternoNoDisponibleException} (→ HTTP 503). Nunca se
 * inventa una tasa.
 *
 * <p>La caché es por instancia y no se persiste: reiniciar el backend la vacía.
 */
@Component
public class CriptoYaClient {

    private static final Logger log = LoggerFactory.getLogger(CriptoYaClient.class);
    private static final int ESCALA = 6;

    private final RestClient restClient;
    private final Duration ttl;
    private final Duration toleranciaMaxima;
    private final Clock clock;

    private final Map<String, Cotizacion> cache = new ConcurrentHashMap<>();

    /** Un {@code bid} y el instante en que se obtuvo de CriptoYa. */
    private record Cotizacion(BigDecimal bid, Instant obtenida) {
    }

    /** El que usa Spring. Explícito porque la clase tiene dos constructores públicos. */
    @Autowired
    public CriptoYaClient(RestClient.Builder builder,
            @Value("${criptoya.base-url}") String baseUrl,
            @Value("${criptoya.cache-ttl:60s}") Duration ttl,
            @Value("${criptoya.cache-max-stale:24h}") Duration toleranciaMaxima) {
        this(builder, baseUrl, ttl, toleranciaMaxima, Clock.systemUTC());
    }

    /**
     * Constructor para tests: permite controlar el paso del tiempo y así envejecer
     * la caché sin esperar. Spring nunca lo usa —elige el otro, que es el único con
     * {@code @Value}—, pero es público porque el test vive en otro paquete.
     */
    public CriptoYaClient(RestClient.Builder builder, String baseUrl, Duration ttl,
            Duration toleranciaMaxima, Clock clock) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.ttl = ttl;
        this.toleranciaMaxima = toleranciaMaxima;
        this.clock = clock;
    }

    /** Precio P2P de un par: {@code {"ask": 7.10, "bid": 6.85, "time": 1234567890}}. */
    public record PrecioP2P(BigDecimal ask, BigDecimal bid, Long time) {
    }

    /**
     * Convierte un monto en moneda fiat a USDT: consulta {@code USDT/{moneda}},
     * {@code tasa = 1 / bid}, {@code montoUsdt = montoOriginal * tasa}.
     */
    public Conversion convertirFiatAUsdt(String moneda, BigDecimal montoOriginal) {
        BigDecimal bid = bid("/USDT/" + moneda + "/1");
        BigDecimal tasa = BigDecimal.ONE.divide(bid, ESCALA, RoundingMode.HALF_UP);
        return new Conversion(aUsdt(montoOriginal, tasa), tasa);
    }

    /**
     * Convierte un monto en criptomoneda a USDT: consulta {@code {moneda}/USD} y
     * {@code USDT/USD}, {@code tasa = bidMoneda / bidUsdt},
     * {@code montoUsdt = montoOriginal * tasa}.
     */
    public Conversion convertirCriptoAUsdt(String moneda, BigDecimal montoOriginal) {
        BigDecimal bidMoneda = bid("/" + moneda + "/USD/1");
        BigDecimal bidUsdt = bid("/USDT/USD/1");
        BigDecimal tasa = bidMoneda.divide(bidUsdt, ESCALA, RoundingMode.HALF_UP);
        return new Conversion(aUsdt(montoOriginal, tasa), tasa);
    }

    /** Vacía la caché. Existe para que los tests partan de un estado conocido. */
    public void vaciarCache() {
        cache.clear();
    }

    private BigDecimal aUsdt(BigDecimal montoOriginal, BigDecimal tasa) {
        return montoOriginal.multiply(tasa).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * El {@code bid} del par, de la caché o de CriptoYa, con la degradación
     * documentada en la clase.
     */
    private BigDecimal bid(String path) {
        Instant ahora = clock.instant();
        Cotizacion cacheada = cache.get(path);

        if (cacheada != null && Duration.between(cacheada.obtenida(), ahora).compareTo(ttl) <= 0) {
            return cacheada.bid();
        }

        try {
            BigDecimal fresco = consultar(path);
            cache.put(path, new Cotizacion(fresco, ahora));
            return fresco;
        } catch (ServicioExternoNoDisponibleException ex) {
            if (cacheada != null
                    && Duration.between(cacheada.obtenida(), ahora).compareTo(toleranciaMaxima) <= 0) {
                log.warn("CriptoYa falló para {}; se usa la cotización cacheada del {}",
                        path, cacheada.obtenida());
                return cacheada.bid();
            }
            throw ex;
        }
    }

    private BigDecimal consultar(String path) {
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
        return precio.bid();
    }
}
