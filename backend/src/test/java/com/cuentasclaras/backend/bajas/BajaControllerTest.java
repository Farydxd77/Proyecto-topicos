package com.cuentasclaras.backend.bajas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifica la capacidad `bajas`: qué pasa cuando alguien deja el grupo debiendo, y
 * la decisión del grupo sobre esa deuda.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=integration-test-secret-1234567890-1234567890-abcdef",
        "spring.jpa.hibernate.ddl-auto=update"
})
class BajaControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ParticipanteRepository participanteRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private long marca;
    private String tokenAna;
    private String tokenBeto;
    private String tokenCarla;
    private String tokenDiego;
    private String tokenExtrano;
    private Long grupoId;
    private Long idAna;
    private Long idBeto;
    private Long idCarla;
    private Long idDiego;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        marca = System.nanoTime();

        tokenAna = registrar("ba-ana-" + marca, "Ana", "Perez", "CI-A" + marca);
        tokenBeto = registrar("ba-beto-" + marca, "Beto", "Lopez", "CI-B" + marca);
        tokenCarla = registrar("ba-carla-" + marca, "Carla", "Diaz", "CI-C" + marca);
        tokenDiego = registrar("ba-diego-" + marca, "Diego", "Ruiz", "CI-D" + marca);
        tokenExtrano = registrar("ba-x-" + marca, "Equis", "Equis", "CI-X" + marca);

        idAna = participanteIdDe("ba-ana-" + marca);
        idBeto = participanteIdDe("ba-beto-" + marca);
        idCarla = participanteIdDe("ba-carla-" + marca);
        idDiego = participanteIdDe("ba-diego-" + marca);

        grupoId = crearGrupo(tokenAna, "Bajas " + marca);
        agregarMiembro(tokenAna, grupoId, idBeto);
        agregarMiembro(tokenAna, grupoId, idCarla);
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

    /** Gasto de `monto` pagado por Ana y repartido entre todos los miembros. */
    private void registrarGasto(String monto) throws Exception {
        String body = """
                {"descripcion":"Cabana","monto":%s,"pagadorId":%d,"fecha":"2026-09-01"}
                """.formatted(monto, idAna);
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void quitar(String token, Long participanteId) throws Exception {
        mockMvc.perform(delete("/api/grupos/{id}/miembros/{pid}", grupoId, participanteId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    private JsonNode bajas(String token) throws Exception {
        String response = mockMvc.perform(get("/api/grupos/{id}/bajas", grupoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private ResultActions resolver(String token, Long bajaId, boolean asumir) throws Exception {
        return mockMvc.perform(put("/api/grupos/{id}/bajas/{bid}", grupoId, bajaId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"asumir\":%s}".formatted(asumir)));
    }

    private Map<String, BigDecimal> balancesPorNombre() throws Exception {
        String json = mockMvc.perform(get("/api/grupos/{id}/balances", grupoId)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, BigDecimal> m = new HashMap<>();
        for (JsonNode e : objectMapper.readTree(json)) {
            m.put(e.get("participante").get("nombre").asString(),
                    new BigDecimal(e.get("balance").asString()));
        }
        return m;
    }

    private BigDecimal sumaBalances() throws Exception {
        return balancesPorNombre().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 1. Registro automático de la baja ------------------------------------

    @Test
    void salirConSaldo_registraBajaPendiente() throws Exception {
        registrarGasto("900.00"); // 300 cada uno; Carla queda en -300
        quitar(tokenAna, idCarla);

        JsonNode lista = bajas(tokenAna);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("participante").get("id").asLong()).isEqualTo(idCarla);
        assertThat(new BigDecimal(lista.get(0).get("saldo").asString()))
                .isEqualByComparingTo("-300.00");
        assertThat(lista.get(0).get("estado").asString()).isEqualTo("PENDIENTE");
        assertThat(lista.get(0).get("reparto")).isEmpty();
    }

    @Test
    void salirSinSaldo_noRegistraBaja() throws Exception {
        // Sin gastos, Carla está en 0.00 al salir.
        quitar(tokenAna, idCarla);

        assertThat(bajas(tokenAna)).isEmpty();
    }

    @Test
    void abandonarPorSuCuentaConSaldo_registraBajaPendiente() throws Exception {
        registrarGasto("900.00");
        // Beto se va solo, no lo echa nadie.
        mockMvc.perform(delete("/api/grupos/{id}/miembros/{pid}", grupoId, idBeto)
                        .header("Authorization", bearer(tokenBeto)))
                .andExpect(status().isNoContent());

        JsonNode lista = bajas(tokenAna);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("participante").get("id").asLong()).isEqualTo(idBeto);
        assertThat(lista.get(0).get("estado").asString()).isEqualTo("PENDIENTE");
    }

    @Test
    void saldoQuedaCongelado() throws Exception {
        registrarGasto("900.00");
        quitar(tokenAna, idCarla);

        // Después de la baja, Beto le paga a Ana: eso mueve balances pero no la baja.
        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenBeto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorId\":%d,\"monto\":300.00,\"fecha\":\"2026-09-05\"}"
                                .formatted(idAna)))
                .andExpect(status().isCreated());

        assertThat(new BigDecimal(bajas(tokenAna).get(0).get("saldo").asString()))
                .isEqualByComparingTo("-300.00");
    }

    // 2. Listado ----------------------------------------------------------

    @Test
    void listar_grupoSinBajas_devuelveListaVacia() throws Exception {
        assertThat(bajas(tokenAna)).isEmpty();
    }

    @Test
    void listar_noMiembro_devuelve403() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/bajas", grupoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/bajas", grupoId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // 3. Resolución -------------------------------------------------------

    @Test
    void resolver_asumir_reparteEntreLosMiembrosActuales() throws Exception {
        registrarGasto("900.00");
        quitar(tokenAna, idCarla);
        Long bajaId = bajas(tokenAna).get(0).get("id").asLong();

        String json = resolver(tokenAna, bajaId, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ASUMIDA"))
                .andExpect(jsonPath("$.reparto.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        for (JsonNode fila : objectMapper.readTree(json).get("reparto")) {
            assertThat(new BigDecimal(fila.get("monto").asString()))
                    .isEqualByComparingTo("-150.00");
        }

        Map<String, BigDecimal> b = balancesPorNombre();
        assertThat(b.get("Carla")).isEqualByComparingTo("0.00");
        assertThat(b.get("Ana")).isEqualByComparingTo("450.00");
        assertThat(b.get("Beto")).isEqualByComparingTo("-450.00");
        assertThat(sumaBalances()).isEqualByComparingTo("0.00");
    }

    @Test
    void resolver_noAsumir_noCambiaNingunBalance() throws Exception {
        registrarGasto("900.00");
        quitar(tokenAna, idCarla);
        Map<String, BigDecimal> antes = balancesPorNombre();
        Long bajaId = bajas(tokenAna).get(0).get("id").asLong();

        resolver(tokenAna, bajaId, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NO_ASUMIDA"))
                .andExpect(jsonPath("$.reparto.length()").value(0));

        assertThat(balancesPorNombre()).isEqualTo(antes);
        assertThat(sumaBalances()).isEqualByComparingTo("0.00");
    }

    @Test
    void resolver_asumirDeAcreedor_subeElBalanceDeLosDemas() throws Exception {
        // Ana pagó todo, así que le deben 600. Ana transfiere el rol y se va.
        registrarGasto("900.00");
        mockMvc.perform(put("/api/grupos/{id}/creador", grupoId)
                        .header("Authorization", bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participanteId\":%d}".formatted(idBeto)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/grupos/{id}/miembros/{pid}", grupoId, idAna)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isNoContent());

        Long bajaId = bajas(tokenBeto).get(0).get("id").asLong();
        resolver(tokenBeto, bajaId, true).andExpect(status().isOk());

        String json = mockMvc.perform(get("/api/grupos/{id}/balances", grupoId)
                        .header("Authorization", bearer(tokenBeto)))
                .andReturn().getResponse().getContentAsString();
        Map<String, BigDecimal> b = new HashMap<>();
        for (JsonNode e : objectMapper.readTree(json)) {
            b.put(e.get("participante").get("nombre").asString(),
                    new BigDecimal(e.get("balance").asString()));
        }
        // Ana queda en cero y los dos que quedan dejan de deberle: suben 300 cada uno.
        assertThat(b.get("Ana")).isEqualByComparingTo("0.00");
        assertThat(b.get("Beto")).isEqualByComparingTo("0.00");
        assertThat(b.get("Carla")).isEqualByComparingTo("0.00");
    }

    @Test
    void resolver_miembroNoCreador_devuelve403() throws Exception {
        registrarGasto("900.00");
        quitar(tokenAna, idCarla);
        Long bajaId = bajas(tokenAna).get(0).get("id").asLong();

        resolver(tokenBeto, bajaId, true)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertThat(bajas(tokenAna).get(0).get("estado").asString()).isEqualTo("PENDIENTE");
    }

    @Test
    void resolver_bajaYaResuelta_devuelve409() throws Exception {
        registrarGasto("900.00");
        quitar(tokenAna, idCarla);
        Long bajaId = bajas(tokenAna).get(0).get("id").asLong();

        resolver(tokenAna, bajaId, true).andExpect(status().isOk());
        resolver(tokenAna, bajaId, false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(bajas(tokenAna).get(0).get("estado").asString()).isEqualTo("ASUMIDA");
    }

    @Test
    void resolver_bajaInexistente_devuelve404() throws Exception {
        resolver(tokenAna, 999_999_999L, true)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void resolver_sinCampoAsumir_devuelve400() throws Exception {
        registrarGasto("900.00");
        quitar(tokenAna, idCarla);
        Long bajaId = bajas(tokenAna).get(0).get("id").asLong();

        mockMvc.perform(put("/api/grupos/{id}/bajas/{bid}", grupoId, bajaId)
                        .header("Authorization", bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.asumir").exists());
    }

    // 4. El reparto queda congelado ---------------------------------------

    @Test
    void repartoCongelado_miembroNuevoNoAbsorbe() throws Exception {
        registrarGasto("900.00");
        quitar(tokenAna, idCarla);
        Long bajaId = bajas(tokenAna).get(0).get("id").asLong();
        resolver(tokenAna, bajaId, true).andExpect(status().isOk());

        // Diego entra DESPUÉS de la decisión.
        agregarMiembro(tokenAna, grupoId, idDiego);

        JsonNode baja = bajas(tokenAna).get(0);
        assertThat(baja.get("reparto").size()).isEqualTo(2);
        for (JsonNode fila : baja.get("reparto")) {
            assertThat(fila.get("participante").get("id").asLong()).isNotEqualTo(idDiego);
        }
        assertThat(balancesPorNombre().get("Diego")).isEqualByComparingTo("0.00");
        assertThat(sumaBalances()).isEqualByComparingTo("0.00");
    }

    @Test
    void resolver_deudaNoDivisible_sumaSigueEnCero() throws Exception {
        // 100.00 entre 3 → Carla queda en -33.33 y se va; se reparte entre 2.
        registrarGasto("100.00");
        quitar(tokenAna, idCarla);
        Long bajaId = bajas(tokenAna).get(0).get("id").asLong();

        String json = resolver(tokenAna, bajaId, true)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        BigDecimal sumaReparto = BigDecimal.ZERO;
        for (JsonNode fila : objectMapper.readTree(json).get("reparto")) {
            sumaReparto = sumaReparto.add(new BigDecimal(fila.get("monto").asString()));
        }
        assertThat(sumaReparto).isEqualByComparingTo("-33.33");
        assertThat(balancesPorNombre().get("Carla")).isEqualByComparingTo("0.00");
        assertThat(sumaBalances()).isEqualByComparingTo("0.00");
    }

    @Test
    void bajasEnLosTresEstados_sumaSigueEnCero() throws Exception {
        agregarMiembro(tokenAna, grupoId, idDiego);
        registrarGasto("1000.00"); // 250 cada uno

        quitar(tokenAna, idCarla);
        Long bajaCarla = bajas(tokenAna).get(0).get("id").asLong();
        resolver(tokenAna, bajaCarla, true).andExpect(status().isOk());

        quitar(tokenAna, idDiego);
        Long bajaDiego = 0L;
        for (JsonNode b : bajas(tokenAna)) {
            if (b.get("participante").get("id").asLong() == idDiego) {
                bajaDiego = b.get("id").asLong();
            }
        }
        resolver(tokenAna, bajaDiego, false).andExpect(status().isOk());

        // Beto se va y su baja queda PENDIENTE.
        mockMvc.perform(delete("/api/grupos/{id}/miembros/{pid}", grupoId, idBeto)
                        .header("Authorization", bearer(tokenBeto)))
                .andExpect(status().isNoContent());

        assertThat(bajas(tokenAna)).hasSize(3);
        assertThat(sumaBalances()).isEqualByComparingTo("0.00");
    }
}
