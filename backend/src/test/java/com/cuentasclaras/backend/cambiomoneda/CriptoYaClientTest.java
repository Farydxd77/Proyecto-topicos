package com.cuentasclaras.backend.cambiomoneda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.cuentasclaras.backend.client.Conversion;
import com.cuentasclaras.backend.client.CriptoYaClient;
import com.cuentasclaras.backend.exception.ServicioExternoNoDisponibleException;

/**
 * Test del cliente HTTP aislado: un {@link MockRestServiceServer} vinculado a un
 * {@link RestClient.Builder} local intercepta las llamadas a CriptoYa. No arranca
 * el contexto de Spring.
 *
 * <p>El reloj es controlable para poder envejecer la caché sin esperar.
 */
class CriptoYaClientTest {

    private static final String BASE = "https://criptoya.test/api/binancep2p";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final Duration TOLERANCIA = Duration.ofHours(24);

    /** Reloj que solo avanza cuando el test se lo pide. */
    private static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-09-08T12:00:00Z");

        void avanzar(Duration cuanto) {
            ahora = ahora.plus(cuanto);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private MockRestServiceServer server;
    private CriptoYaClient client;
    private RelojManual reloj;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        reloj = new RelojManual();
        client = new CriptoYaClient(builder, BASE, TTL, TOLERANCIA, reloj);
    }

    private static String cuerpoConBid(String bid) {
        return "{\"ask\":7.10,\"bid\":%s,\"time\":1}".formatted(bid);
    }

    // 6.1 Conversión fiat -------------------------------------------------

    @Test
    void convertirFiatAUsdt_usaElBidYCalculaTasaYMonto() {
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(cuerpoConBid("6.85"), MediaType.APPLICATION_JSON));

        Conversion c = client.convertirFiatAUsdt("BOB", new BigDecimal("800.00"));

        // tasa = 1 / 6.85 redondeado a 6 decimales
        assertThat(c.tasaCambio()).isEqualByComparingTo("0.145985");
        // montoUsdt = 800.00 * 0.145985
        assertThat(c.montoUsdt()).isEqualByComparingTo("116.788000");
        server.verify();
    }

    // 6.2 Conversión cripto (dos consultas) ----------------------------

    @Test
    void convertirCriptoAUsdt_combinaLosDosBids() {
        server.expect(requestTo(BASE + "/BTC/USD/1"))
                .andRespond(withSuccess(
                        "{\"ask\":60100,\"bid\":60000.00,\"time\":1}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/USDT/USD/1"))
                .andRespond(withSuccess(
                        "{\"ask\":1.01,\"bid\":1.00,\"time\":1}", MediaType.APPLICATION_JSON));

        Conversion c = client.convertirCriptoAUsdt("BTC", new BigDecimal("0.01"));

        assertThat(c.tasaCambio()).isEqualByComparingTo("60000");
        assertThat(c.montoUsdt()).isEqualByComparingTo("600.000000");
        server.verify();
    }

    // 6.3 Fallos de la API externa ----------------------------------

    @Test
    void convertirFiatAUsdt_servidorDevuelve500_lanzaServicioExternoNoDisponible() {
        server.expect(requestTo(BASE + "/USDT/BOB/1")).andRespond(withServerError());

        assertThatThrownBy(() -> client.convertirFiatAUsdt("BOB", new BigDecimal("800.00")))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
    }

    @Test
    void convertirFiatAUsdt_respuestaSinBid_lanzaServicioExternoNoDisponible() {
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andRespond(withSuccess("{\"ask\":7.10}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.convertirFiatAUsdt("BOB", new BigDecimal("800.00")))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
    }

    // 6.4 Caché ---------------------------------------------------------

    @Test
    void dosConversionesSeguidas_consultanUnaSolaVez() {
        // Una sola expectativa: si el cliente consultara dos veces, la segunda
        // llamada fallaría por falta de expectativa.
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andRespond(withSuccess(cuerpoConBid("6.85"), MediaType.APPLICATION_JSON));

        Conversion primera = client.convertirFiatAUsdt("BOB", new BigDecimal("100.00"));
        Conversion segunda = client.convertirFiatAUsdt("BOB", new BigDecimal("200.00"));

        assertThat(segunda.tasaCambio()).isEqualByComparingTo(primera.tasaCambio());
        server.verify();
    }

    @Test
    void cotizacionVencida_vuelveAConsultar() {
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andRespond(withSuccess(cuerpoConBid("6.85"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andRespond(withSuccess(cuerpoConBid("7.00"), MediaType.APPLICATION_JSON));

        Conversion antes = client.convertirFiatAUsdt("BOB", new BigDecimal("100.00"));
        reloj.avanzar(TTL.plusSeconds(1));
        Conversion despues = client.convertirFiatAUsdt("BOB", new BigDecimal("100.00"));

        assertThat(antes.tasaCambio()).isEqualByComparingTo("0.145985");
        assertThat(despues.tasaCambio()).isEqualByComparingTo("0.142857");
        server.verify();
    }

    @Test
    void monedasDistintas_consultanCadaPar() {
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andRespond(withSuccess(cuerpoConBid("6.85"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/USDT/ARS/1"))
                .andRespond(withSuccess(cuerpoConBid("1000.00"), MediaType.APPLICATION_JSON));

        Conversion bob = client.convertirFiatAUsdt("BOB", new BigDecimal("100.00"));
        Conversion ars = client.convertirFiatAUsdt("ARS", new BigDecimal("100.00"));

        assertThat(bob.tasaCambio()).isEqualByComparingTo("0.145985");
        assertThat(ars.tasaCambio()).isEqualByComparingTo("0.001000");
        server.verify();
    }

    @Test
    void falloConCotizacionCacheada_usaLaCacheada() {
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andRespond(withSuccess(cuerpoConBid("6.85"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/USDT/BOB/1")).andRespond(withServerError());

        Conversion antes = client.convertirFiatAUsdt("BOB", new BigDecimal("100.00"));
        reloj.avanzar(TTL.plusSeconds(1));
        Conversion durante = client.convertirFiatAUsdt("BOB", new BigDecimal("100.00"));

        // La caída de CriptoYa no impide registrar el gasto: se aplica la última
        // tasa conocida, y es esa la que después se persiste.
        assertThat(durante.tasaCambio()).isEqualByComparingTo(antes.tasaCambio());
        server.verify();
    }

    @Test
    void falloSinCotizacionCacheada_lanzaServicioExternoNoDisponible() {
        server.expect(requestTo(BASE + "/USDT/BOB/1")).andRespond(withServerError());

        assertThatThrownBy(() -> client.convertirFiatAUsdt("BOB", new BigDecimal("100.00")))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
        server.verify();
    }

    @Test
    void falloConCotizacionDemasiadoVieja_lanzaServicioExternoNoDisponible() {
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andRespond(withSuccess(cuerpoConBid("6.85"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/USDT/BOB/1")).andRespond(withServerError());

        client.convertirFiatAUsdt("BOB", new BigDecimal("100.00"));
        reloj.avanzar(TOLERANCIA.plusSeconds(1));

        assertThatThrownBy(() -> client.convertirFiatAUsdt("BOB", new BigDecimal("100.00")))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
        server.verify();
    }

    @Test
    void vaciarCache_obligaAConsultarDeNuevo() {
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andRespond(withSuccess(cuerpoConBid("6.85"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andRespond(withSuccess(cuerpoConBid("7.00"), MediaType.APPLICATION_JSON));

        client.convertirFiatAUsdt("BOB", new BigDecimal("100.00"));
        client.vaciarCache();
        Conversion despues = client.convertirFiatAUsdt("BOB", new BigDecimal("100.00"));

        assertThat(despues.tasaCambio()).isEqualByComparingTo("0.142857");
        server.verify();
    }
}
