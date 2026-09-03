package com.cuentasclaras.backend.balances;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=integration-test-secret-1234567890-1234567890-abcdef",
        "spring.jpa.hibernate.ddl-auto=update"
})
class BalanceControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ParticipanteRepository participanteRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private String tokenAna;
    private String tokenExtrano;
    private Long grupoId;
    private Long idAna;
    private Long idBeto;
    private Long idCarla;
    private Long idDiego;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long marca = System.nanoTime();

        tokenAna = registrar("bal-ana-" + marca, "Ana", "Perez", "CI-A" + marca);
        registrar("bal-beto-" + marca, "Beto", "Lopez", "CI-B" + marca);
        registrar("bal-carla-" + marca, "Carla", "Diaz", "CI-C" + marca);
        registrar("bal-diego-" + marca, "Diego", "Ruiz", "CI-D" + marca);
        tokenExtrano = registrar("bal-x-" + marca, "Equis", "Equis", "CI-X" + marca);

        idAna = participanteIdDe("bal-ana-" + marca);
        idBeto = participanteIdDe("bal-beto-" + marca);
        idCarla = participanteIdDe("bal-carla-" + marca);
        idDiego = participanteIdDe("bal-diego-" + marca);

        grupoId = crearGrupo(tokenAna, "Samaipata " + marca);
        agregarMiembro(tokenAna, grupoId, idBeto);
        agregarMiembro(tokenAna, grupoId, idCarla);
        agregarMiembro(tokenAna, grupoId, idDiego);
    }

    // Helpers -------------------------------------------------------------

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

    private void registrarGastoSamaipata() throws Exception {
        String body = """
                {"descripcion":"Cabana","monto":800.00,"pagadorId":%d,"fecha":"2026-09-01"}
                """.formatted(idAna);
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private Map<String, BigDecimal> balancesPorNombre(String json) {
        Map<String, BigDecimal> m = new HashMap<>();
        for (JsonNode e : objectMapper.readTree(json)) {
            m.put(e.get("participante").get("nombre").asString(),
                    new BigDecimal(e.get("balance").asString()));
        }
        return m;
    }

    private BigDecimal sumaBalances(String json) {
        BigDecimal s = BigDecimal.ZERO;
        for (JsonNode e : objectMapper.readTree(json)) {
            s = s.add(new BigDecimal(e.get("balance").asString()));
        }
        return s;
    }

    // 6.1 Balances: escenario Samaipata --------------------------------

    @Test
    void balances_escenarioSamaipata_devuelveBalancesCorrectos() throws Exception {
        registrarGastoSamaipata();

        String response = mockMvc.perform(get("/api/grupos/{id}/balances", grupoId)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andReturn().getResponse().getContentAsString();

        Map<String, BigDecimal> balances = balancesPorNombre(response);
        assertThat(balances.get("Ana")).isEqualByComparingTo("600.00");
        assertThat(balances.get("Beto")).isEqualByComparingTo("-200.00");
        assertThat(balances.get("Carla")).isEqualByComparingTo("-200.00");
        assertThat(balances.get("Diego")).isEqualByComparingTo("-200.00");
        assertThat(sumaBalances(response)).isEqualByComparingTo("0.00");
        assertThat(response).doesNotContain("password").doesNotContain("secret123");
    }

    // 6.2 Balances: grupo sin gastos y suma cero ----------------------

    @Test
    void balances_grupoSinGastos_todosEnCero() throws Exception {
        String response = mockMvc.perform(get("/api/grupos/{id}/balances", grupoId)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andReturn().getResponse().getContentAsString();

        balancesPorNombre(response).values()
                .forEach(b -> assertThat(b).isEqualByComparingTo("0.00"));
        assertThat(sumaBalances(response)).isEqualByComparingTo("0.00");
    }

    // 6.3 Balances: autorización -------------------------------------

    @Test
    void balances_usuarioNoMiembro_devuelve403() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/balances", grupoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void balances_grupoInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/balances", 999_999_999L)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void balances_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/balances", grupoId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/grupos/" + grupoId + "/balances"));
    }

    // 6.4 Liquidación: escenario Samaipata --------------------------

    @Test
    void liquidacion_escenarioSamaipata_tresTransferenciasDe200HaciaAna() throws Exception {
        registrarGastoSamaipata();

        String response = mockMvc.perform(get("/api/grupos/{id}/liquidacion", grupoId)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        int haciaAna = 0;
        for (JsonNode t : objectMapper.readTree(response)) {
            assertThat(t.get("para").asString()).isEqualTo("Ana");
            assertThat(t.get("paraId").asLong()).isEqualTo(idAna);
            assertThat(new BigDecimal(t.get("monto").asString())).isEqualByComparingTo("200.00");
            assertThat(t.get("de").asString()).isIn("Beto", "Carla", "Diego");
            assertThat(t.get("deId").asLong()).isIn(idBeto, idCarla, idDiego);
            haciaAna++;
        }
        assertThat(haciaAna).isEqualTo(3);
    }

    // 6.5 Liquidación: vacía y autorización ------------------------

    @Test
    void liquidacion_grupoSinGastos_listaVacia() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/liquidacion", grupoId)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void liquidacion_usuarioNoMiembro_devuelve403() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/liquidacion", grupoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden());
    }

    @Test
    void liquidacion_grupoInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/liquidacion", 999_999_999L)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isNotFound());
    }

    @Test
    void liquidacion_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/liquidacion", grupoId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
