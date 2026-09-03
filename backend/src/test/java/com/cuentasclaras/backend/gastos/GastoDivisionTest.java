package com.cuentasclaras.backend.gastos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
class GastoDivisionTest {

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
    private String tokenA;
    private String tokenB;
    private String tokenC;
    private Long pIdA;
    private Long pIdB;
    private Long pIdC;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        marca = System.nanoTime();

        tokenA = registrar("gd-a-" + marca, "Ana", "Perez", "CI-A" + marca);
        tokenB = registrar("gd-b-" + marca, "Beto", "Lopez", "CI-B" + marca);
        tokenC = registrar("gd-c-" + marca, "Caro", "Diaz", "CI-C" + marca);

        pIdA = participanteIdDe("gd-a-" + marca);
        pIdB = participanteIdDe("gd-b-" + marca);
        pIdC = participanteIdDe("gd-c-" + marca);
    }

    // Helpers -------------------------------------------------------

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

    private String registrarGasto(String token, Long grupo, String monto, Long pagadorId) throws Exception {
        String body = """
                {"descripcion":"Gasto","monto":%s,"pagadorId":%d,"fecha":"2026-09-01"}
                """.formatted(monto, pagadorId);
        return mockMvc.perform(post("/api/grupos/{id}/gastos", grupo)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private BigDecimal sumaDivision(JsonNode gasto) {
        BigDecimal suma = BigDecimal.ZERO;
        for (JsonNode e : gasto.get("division")) {
            suma = suma.add(new BigDecimal(e.get("montoAdeudado").asString()));
        }
        return suma;
    }

    private BigDecimal adeudadoDe(JsonNode gasto, Long participanteId) {
        for (JsonNode e : gasto.get("division")) {
            if (e.get("participante").get("id").asLong() == participanteId) {
                return new BigDecimal(e.get("montoAdeudado").asString());
            }
        }
        throw new AssertionError("El participante " + participanteId + " no está en la división");
    }

    // 8.1 Aritmética ------------------------------------------

    @Test
    void division_noExacta_pagadorAbsorbeElCentavo() throws Exception {
        Long grupo = crearGrupo(tokenA, "G3-" + marca);
        agregarMiembro(tokenA, grupo, pIdB);
        agregarMiembro(tokenA, grupo, pIdC);

        JsonNode gasto = objectMapper.readTree(registrarGasto(tokenA, grupo, "100.00", pIdA));

        assertThat(gasto.get("division")).hasSize(3);
        assertThat(adeudadoDe(gasto, pIdB)).isEqualByComparingTo("33.33");
        assertThat(adeudadoDe(gasto, pIdC)).isEqualByComparingTo("33.33");
        assertThat(adeudadoDe(gasto, pIdA)).isEqualByComparingTo("33.34");
        assertThat(sumaDivision(gasto)).isEqualByComparingTo("100.00");
    }

    @Test
    void division_exacta_todosIgual() throws Exception {
        Long grupo = crearGrupo(tokenA, "G3e-" + marca);
        agregarMiembro(tokenA, grupo, pIdB);
        agregarMiembro(tokenA, grupo, pIdC);

        JsonNode gasto = objectMapper.readTree(registrarGasto(tokenA, grupo, "90.00", pIdA));

        assertThat(gasto.get("division")).hasSize(3);
        assertThat(adeudadoDe(gasto, pIdA)).isEqualByComparingTo("30.00");
        assertThat(adeudadoDe(gasto, pIdB)).isEqualByComparingTo("30.00");
        assertThat(adeudadoDe(gasto, pIdC)).isEqualByComparingTo("30.00");
        assertThat(sumaDivision(gasto)).isEqualByComparingTo("90.00");
    }

    @Test
    void division_grupoDeUnMiembro_adeudaTodo() throws Exception {
        Long grupo = crearGrupo(tokenA, "G1-" + marca);

        JsonNode gasto = objectMapper.readTree(registrarGasto(tokenA, grupo, "50.00", pIdA));

        assertThat(gasto.get("division")).hasSize(1);
        assertThat(adeudadoDe(gasto, pIdA)).isEqualByComparingTo("50.00");
    }

    // 8.2 Aislamiento frente a cambios de miembros --------

    @Test
    void gastoAnterior_noSeVeAfectadoPorUnNuevoMiembro() throws Exception {
        Long grupo = crearGrupo(tokenA, "Giso-" + marca);
        agregarMiembro(tokenA, grupo, pIdB);

        Long gastoId = objectMapper.readTree(registrarGasto(tokenA, grupo, "100.00", pIdA)).get("id").asLong();

        agregarMiembro(tokenA, grupo, pIdC);

        JsonNode anterior = objectMapper.readTree(mockMvc.perform(
                        get("/api/grupos/{id}/gastos/{gid}", grupo, gastoId)
                                .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(anterior.get("division")).hasSize(2);
        assertThat(adeudadoDe(anterior, pIdA)).isEqualByComparingTo("50.00");
        assertThat(adeudadoDe(anterior, pIdB)).isEqualByComparingTo("50.00");

        JsonNode nuevo = objectMapper.readTree(registrarGasto(tokenA, grupo, "90.00", pIdA));
        assertThat(nuevo.get("division")).hasSize(3);
        assertThat(sumaDivision(nuevo)).isEqualByComparingTo("90.00");
    }

    // 8.3 Recálculo en la edición -----------------------

    @Test
    void editar_recalculaDivisionConNuevoMonto() throws Exception {
        Long grupo = crearGrupo(tokenA, "Gedit-" + marca);
        agregarMiembro(tokenA, grupo, pIdB);
        agregarMiembro(tokenA, grupo, pIdC);

        Long gastoId = objectMapper.readTree(registrarGasto(tokenA, grupo, "100.00", pIdA)).get("id").asLong();

        String body = """
                {"descripcion":"Gasto","monto":10.00,"pagadorId":%d,"fecha":"2026-09-02"}
                """.formatted(pIdA);
        JsonNode editado = objectMapper.readTree(mockMvc.perform(
                        put("/api/grupos/{id}/gastos/{gid}", grupo, gastoId)
                                .header("Authorization", bearer(tokenB))
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(editado.get("division")).hasSize(3);
        assertThat(sumaDivision(editado)).isEqualByComparingTo("10.00");
        assertThat(adeudadoDe(editado, pIdB)).isEqualByComparingTo("3.33");
        assertThat(adeudadoDe(editado, pIdC)).isEqualByComparingTo("3.33");
        assertThat(adeudadoDe(editado, pIdA)).isEqualByComparingTo("3.34");
    }
}
