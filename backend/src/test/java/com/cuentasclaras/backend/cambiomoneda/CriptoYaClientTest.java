package com.cuentasclaras.backend.cambiomoneda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

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
 */
class CriptoYaClientTest {

    private static final String BASE = "https://criptoya.test/api/binancep2p";

    private MockRestServiceServer server;
    private CriptoYaClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CriptoYaClient(builder, BASE);
    }

    // 6.1 Conversión fiat -------------------------------------------------

    @Test
    void convertirFiatAUsdt_usaElBidYCalculaTasaYMonto() {
        server.expect(requestTo(BASE + "/USDT/BOB/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"ask\":7.10,\"bid\":6.85,\"time\":1}", MediaType.APPLICATION_JSON));

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
}
