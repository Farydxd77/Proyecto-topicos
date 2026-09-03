package com.cuentasclaras.backend.gastos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.cuentasclaras.backend.client.Conversion;
import com.cuentasclaras.backend.client.CriptoYaClient;
import com.cuentasclaras.backend.exception.ServicioExternoNoDisponibleException;
import com.cuentasclaras.backend.repository.GastoRepository;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "jwt.secret=integration-test-secret-1234567890-1234567890-abcdef",
        "spring.jpa.hibernate.ddl-auto=update"
})
class GastoMonedaControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ParticipanteRepository participanteRepository;
    @Autowired
    private GastoRepository gastoRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private CriptoYaClient criptoYaClient;

    private MockMvc mockMvc;

    private String tokenA;
    private String tokenExtrano;
    private Long grupoId;
    private Long pIdA;
    private Long pIdB;

    /**
     * Hibernate `ddl-auto=update` no cambia el tipo de columnas existentes; el
     * spec amplía `gastos.monto` a `numeric(20,8)` y `monto_usdt` / `tasa_cambio`
     * a `numeric(20,6)` (la tasa cripto no cabe en `numeric(10,6)`) con un `ALTER`
     * manual. Se aplica aquí (idempotente) para probar el caso cripto.
     */
    @BeforeAll
    void ampliarColumnas() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("ALTER TABLE gastos ALTER COLUMN monto TYPE numeric(20,8)");
            s.execute("ALTER TABLE gastos ALTER COLUMN monto_usdt TYPE numeric(20,6)");
            s.execute("ALTER TABLE gastos ALTER COLUMN tasa_cambio TYPE numeric(20,6)");
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long marca = System.nanoTime();

        tokenA = registrar("cm-a-" + marca, "Ana", "Perez", "CI-A" + marca);
        registrar("cm-b-" + marca, "Beto", "Lopez", "CI-B" + marca);
        tokenExtrano = registrar("cm-x-" + marca, "Equis", "Equis", "CI-X" + marca);

        pIdA = participanteIdDe("cm-a-" + marca);
        pIdB = participanteIdDe("cm-b-" + marca);

        grupoId = crearGrupo(tokenA, "Viaje " + marca);
        agregarMiembro(tokenA, grupoId, pIdB);
    }

    // Helpers -----------------------------------------------------------

    private String registrar(String user, String nombre, String apellido, String ci) throws Exception {
        String body = """
                {"username":"%s","password":"secret123","nombre":"%s","apellido":"%s","ci":"%s"}
                """.formatted(user, nombre, apellido, ci);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long participanteIdDe(String user) {
        Long usuarioId = usuarioRepository.findByUsername(user).orElseThrow().getId();
        return participanteRepository.findByUsuarioId(usuarioId).orElseThrow().getId();
    }

    private Long crearGrupo(String token, String nombre) throws Exception {
        String response = mockMvc.perform(post("/api/grupos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s\"}".formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void agregarMiembro(String token, Long grupo, Long participanteId) throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/miembros", grupo)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participanteId\":%d}".formatted(participanteId)))
                .andExpect(status().isCreated());
    }

    private String postGasto(String bodyJson) throws Exception {
        return mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(bodyJson))
                .andReturn().getResponse().getContentAsString();
    }

    private BigDecimal sumaDivision(String json) {
        BigDecimal s = BigDecimal.ZERO;
        for (JsonNode e : objectMapper.readTree(json).get("division")) {
            s = s.add(new BigDecimal(e.get("montoAdeudado").asString()));
        }
        return s;
    }

    private BigDecimal num(String json, String field) {
        return new BigDecimal(objectMapper.readTree(json).get(field).asString());
    }

    // 7.1 Registro con moneda fiat ---------------------------------

    @Test
    void registrar_monedaFiat_convierteYPersisteLosDatosDeConversion() throws Exception {
        given(criptoYaClient.convertirFiatAUsdt(eq("BOB"), any()))
                .willReturn(new Conversion(new BigDecimal("116.788321"), new BigDecimal("0.145985")));

        String body = """
                {"descripcion":"Cabana","monto":800.00,"moneda":"BOB","monedaNombre":"Boliviano","pagadorId":%d,"fecha":"2026-09-03"}
                """.formatted(pIdA);
        String response = mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moneda").value("BOB"))
                .andExpect(jsonPath("$.monedaNombre").value("Boliviano"))
                .andReturn().getResponse().getContentAsString();

        assertThat(num(response, "monto")).isEqualByComparingTo("800.00");
        assertThat(num(response, "montoUsdt")).isEqualByComparingTo("116.788321");
        assertThat(num(response, "tasaCambio")).isEqualByComparingTo("0.145985");
        // La división es sobre montoUsdt redondeado a 2 decimales.
        assertThat(sumaDivision(response)).isEqualByComparingTo("116.79");
        assertThat(response).doesNotContain("password");

        Long gastoId = objectMapper.readTree(response).get("id").asLong();
        var gasto = gastoRepository.findById(gastoId).orElseThrow();
        assertThat(gasto.getMoneda()).isEqualTo("BOB");
        assertThat(gasto.getMonedaNombre()).isEqualTo("Boliviano");
        assertThat(gasto.getMontoUsdt()).isEqualByComparingTo("116.788321");
        assertThat(gasto.getTasaCambio()).isEqualByComparingTo("0.145985");
        assertThat(gasto.getMonto()).isEqualByComparingTo("800.00");
    }

    // 7.2 USDT implícito y explícito -----------------------------

    @Test
    void registrar_sinMoneda_esUsdtSinLlamadaExterna() throws Exception {
        String body = """
                {"descripcion":"Cena","monto":100.00,"pagadorId":%d,"fecha":"2026-09-03"}
                """.formatted(pIdA);
        String response = mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moneda").value("USDT"))
                .andExpect(jsonPath("$.monedaNombre").value("Tether"))
                .andReturn().getResponse().getContentAsString();

        assertThat(num(response, "tasaCambio")).isEqualByComparingTo("1");
        assertThat(num(response, "montoUsdt")).isEqualByComparingTo("100.00");
        verifyNoInteractions(criptoYaClient);
    }

    @Test
    void registrar_monedaUsdtExplicita_noLlamaCriptoYa() throws Exception {
        String body = """
                {"descripcion":"Cena","monto":50.00,"moneda":"USDT","pagadorId":%d,"fecha":"2026-09-03"}
                """.formatted(pIdA);
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moneda").value("USDT"))
                .andExpect(jsonPath("$.monedaNombre").value("Tether"))
                .andExpect(jsonPath("$.tasaCambio").value(1));
        verifyNoInteractions(criptoYaClient);
    }

    // 7.3 Moneda no soportada y CriptoYa caído -----------------

    @Test
    void registrar_monedaNoSoportada_devuelve400SinLlamada() throws Exception {
        String body = """
                {"descripcion":"Cena","monto":100.00,"moneda":"XYZ","pagadorId":%d,"fecha":"2026-09-03"}
                """.formatted(pIdA);
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(criptoYaClient);
    }

    @Test
    void registrar_criptoYaCaido_devuelve503YNoRegistraGasto() throws Exception {
        given(criptoYaClient.convertirCriptoAUsdt(eq("BTC"), any()))
                .willThrow(new ServicioExternoNoDisponibleException("CriptoYa no está disponible"));

        String body = """
                {"descripcion":"Cena","monto":0.02,"moneda":"BTC","pagadorId":%d,"fecha":"2026-09-03"}
                """.formatted(pIdA);
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/grupos/" + grupoId + "/gastos"));

        mockMvc.perform(get("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // 7.4 Cripto con 8 decimales -------------------------------

    @Test
    void registrar_montoCriptoCon8Decimales_seConservaSinPerdida() throws Exception {
        given(criptoYaClient.convertirCriptoAUsdt(eq("BTC"), any()))
                .willReturn(new Conversion(new BigDecimal("74.074000"), new BigDecimal("60000")));

        String body = """
                {"descripcion":"Fee","monto":0.00123456,"moneda":"BTC","monedaNombre":"Bitcoin","pagadorId":%d,"fecha":"2026-09-03"}
                """.formatted(pIdA);
        String response = mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moneda").value("BTC"))
                .andReturn().getResponse().getContentAsString();

        assertThat(num(response, "monto")).isEqualByComparingTo("0.00123456");

        Long gastoId = objectMapper.readTree(response).get("id").asLong();
        assertThat(gastoRepository.findById(gastoId).orElseThrow().getMonto())
                .isEqualByComparingTo("0.00123456");
    }

    // 7.5 Edición recalcula la conversión ---------------------

    @Test
    void editar_recalculaConversionYDivision() throws Exception {
        given(criptoYaClient.convertirFiatAUsdt(eq("BOB"), any()))
                .willReturn(new Conversion(new BigDecimal("116.788321"), new BigDecimal("0.145985")))
                .willReturn(new Conversion(new BigDecimal("145.985401"), new BigDecimal("0.145985")));

        Long gastoId = objectMapper.readTree(postGasto("""
                {"descripcion":"Cabana","monto":800.00,"moneda":"BOB","monedaNombre":"Boliviano","pagadorId":%d,"fecha":"2026-09-03"}
                """.formatted(pIdA))).get("id").asLong();

        String response = mockMvc.perform(put("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"Cabana","monto":1000.00,"moneda":"BOB","monedaNombre":"Boliviano","pagadorId":%d,"fecha":"2026-09-04"}
                                """.formatted(pIdA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(num(response, "montoUsdt")).isEqualByComparingTo("145.985401");
        assertThat(sumaDivision(response)).isEqualByComparingTo("145.99");
    }

    // 7.6 Balances siguen en USDT ---------------------------

    @Test
    void balances_conGastoConvertido_sumaCeroYSinCamposDeMoneda() throws Exception {
        given(criptoYaClient.convertirFiatAUsdt(eq("BOB"), any()))
                .willReturn(new Conversion(new BigDecimal("116.788321"), new BigDecimal("0.145985")));
        postGasto("""
                {"descripcion":"Cabana","monto":800.00,"moneda":"BOB","monedaNombre":"Boliviano","pagadorId":%d,"fecha":"2026-09-03"}
                """.formatted(pIdA));

        String response = mockMvc.perform(get("/api/grupos/{id}/balances", grupoId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        BigDecimal suma = BigDecimal.ZERO;
        for (JsonNode e : objectMapper.readTree(response)) {
            suma = suma.add(new BigDecimal(e.get("balance").asString()));
        }
        assertThat(suma).isEqualByComparingTo("0.00");
        assertThat(response).doesNotContain("tasaCambio").doesNotContain("moneda");
    }
}
