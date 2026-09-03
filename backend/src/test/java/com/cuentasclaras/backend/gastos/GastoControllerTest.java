package com.cuentasclaras.backend.gastos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class GastoControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ParticipanteRepository participanteRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private String usuarioA;
    private String tokenA;
    private String usuarioB;
    private String tokenB;
    private String tokenExtrano;

    private Long grupoId;
    private Long pIdA;
    private Long pIdB;
    private Long pIdExtrano;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long marca = System.nanoTime();

        usuarioA = "ga-a-" + marca;
        tokenA = registrar(usuarioA, "Ana", "Perez", "CI-A" + marca);
        usuarioB = "ga-b-" + marca;
        tokenB = registrar(usuarioB, "Beto", "Lopez", "CI-B" + marca);
        tokenExtrano = registrar("ga-x-" + marca, "Caro", "Diaz", "CI-X" + marca);

        pIdA = participanteIdDe(usuarioA);
        pIdB = participanteIdDe(usuarioB);
        pIdExtrano = participanteIdDe("ga-x-" + marca);

        grupoId = crearGrupo(tokenA, "Viaje " + marca);
        agregarMiembro(tokenA, grupoId, pIdB);
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

    private void agregarMiembro(String tokenCreador, Long grupo, Long participanteId) throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/miembros", grupo)
                        .header("Authorization", bearer(tokenCreador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participanteId\":%d}".formatted(participanteId)))
                .andExpect(status().isCreated());
    }

    private String gastoBody(String descripcion, String monto, Long pagadorId, String fecha) {
        return """
                {"descripcion":"%s","monto":%s,"pagadorId":%d,"fecha":"%s"}
                """.formatted(descripcion, monto, pagadorId, fecha);
    }

    private String registrarGasto(String token, Long grupo, String monto, Long pagadorId, String fecha)
            throws Exception {
        return mockMvc.perform(post("/api/grupos/{id}/gastos", grupo)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", monto, pagadorId, fecha)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private BigDecimal sumaDivision(String json) {
        BigDecimal suma = BigDecimal.ZERO;
        for (JsonNode e : objectMapper.readTree(json).get("division")) {
            suma = suma.add(new BigDecimal(e.get("montoAdeudado").asString()));
        }
        return suma;
    }

    // 7.1 Registro ------------------------------------------------------

    @Test
    void registrar_datosValidos_devuelve201ConGastoYDivision() throws Exception {
        String response = mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "100.00", pIdA, "2026-09-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.grupoId").value(grupoId))
                .andExpect(jsonPath("$.descripcion").value("Cena"))
                .andExpect(jsonPath("$.monto").isNumber())
                .andExpect(jsonPath("$.pagador.username").value(usuarioA))
                .andExpect(jsonPath("$.fecha").value("2026-09-01"))
                .andExpect(jsonPath("$.division.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password").doesNotContain("secret123");
        assertThat(sumaDivision(response)).isEqualByComparingTo("100.00");
    }

    @Test
    void registrar_montoCero_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "0.00", pIdA, "2026-09-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/grupos/" + grupoId + "/gastos"));
    }

    @Test
    void registrar_montoNegativo_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "-5.00", pIdA, "2026-09-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrar_descripcionEnBlanco_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("   ", "100.00", pIdA, "2026-09-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.descripcion").isNotEmpty());
    }

    @Test
    void registrar_fechaAusente_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Cena\",\"monto\":100.00,\"pagadorId\":%d}".formatted(pIdA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fecha").isNotEmpty());
    }

    @Test
    void registrar_pagadorIdAusente_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Cena\",\"monto\":100.00,\"fecha\":\"2026-09-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pagadorId").isNotEmpty());
    }

    @Test
    void registrar_pagadorNoEsMiembro_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "100.00", pIdExtrano, "2026-09-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registrar_usuarioNoMiembro_devuelve403() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenExtrano))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "100.00", pIdA, "2026-09-01")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void registrar_grupoInexistente_devuelve404() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/gastos", 999_999_999L)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "100.00", pIdA, "2026-09-01")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 7.2 Listado -----------------------------------------------------

    @Test
    void listar_grupoConGastos_devuelve200OrdenadoPorFechaDesc() throws Exception {
        registrarGasto(tokenA, grupoId, "10.00", pIdA, "2026-09-01");
        registrarGasto(tokenA, grupoId, "20.00", pIdB, "2026-09-05");

        mockMvc.perform(get("/api/grupos/{id}/gastos", grupoId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fecha").value("2026-09-05"))
                .andExpect(jsonPath("$[1].fecha").value("2026-09-01"))
                .andExpect(jsonPath("$[0].pagador.username").value(usuarioB))
                .andExpect(jsonPath("$[0].descripcion").value("Cena"));
    }

    @Test
    void listar_grupoSinGastos_devuelve200ConArrayVacio() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/gastos", grupoId).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listar_usuarioNoMiembro_devuelve403() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listar_grupoInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/gastos", 999_999_999L)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    // 7.3 Detalle ---------------------------------------------------

    @Test
    void detalle_gastoDelGrupo_devuelve200ConDivision() throws Exception {
        Long gastoId = objectMapper.readTree(
                registrarGasto(tokenA, grupoId, "100.00", pIdA, "2026-09-01")).get("id").asLong();

        String response = mockMvc.perform(get("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gastoId))
                .andExpect(jsonPath("$.division.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password");
        assertThat(sumaDivision(response)).isEqualByComparingTo("100.00");
    }

    @Test
    void detalle_gastoInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/gastos/{gid}", grupoId, 999_999_999L)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void detalle_gastoDeOtroGrupo_devuelve404() throws Exception {
        Long otroGrupo = crearGrupo(tokenA, "Otro");
        Long gastoOtro = objectMapper.readTree(
                registrarGasto(tokenA, otroGrupo, "40.00", pIdA, "2026-09-02")).get("id").asLong();

        mockMvc.perform(get("/api/grupos/{id}/gastos/{gid}", grupoId, gastoOtro)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void detalle_usuarioNoMiembro_devuelve403() throws Exception {
        Long gastoId = objectMapper.readTree(
                registrarGasto(tokenA, grupoId, "100.00", pIdA, "2026-09-01")).get("id").asLong();

        mockMvc.perform(get("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden());
    }

    // 7.4 Edición -------------------------------------------------

    @Test
    void editar_datosValidos_devuelve200YRecalculaDivision() throws Exception {
        Long gastoId = objectMapper.readTree(
                registrarGasto(tokenA, grupoId, "100.00", pIdA, "2026-09-01")).get("id").asLong();

        String response = mockMvc.perform(put("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Almuerzo", "60.00", pIdB, "2026-09-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Almuerzo"))
                .andExpect(jsonPath("$.pagador.username").value(usuarioB))
                .andExpect(jsonPath("$.fecha").value("2026-09-03"))
                .andExpect(jsonPath("$.division.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(sumaDivision(response)).isEqualByComparingTo("60.00");
    }

    @Test
    void editar_montoInvalido_devuelve400YGastoSinCambios() throws Exception {
        Long gastoId = objectMapper.readTree(
                registrarGasto(tokenA, grupoId, "100.00", pIdA, "2026-09-01")).get("id").asLong();

        mockMvc.perform(put("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Almuerzo", "0.00", pIdA, "2026-09-03")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Cena"))
                .andExpect(jsonPath("$.fecha").value("2026-09-01"));
    }

    @Test
    void editar_pagadorNoMiembro_devuelve400() throws Exception {
        Long gastoId = objectMapper.readTree(
                registrarGasto(tokenA, grupoId, "100.00", pIdA, "2026-09-01")).get("id").asLong();

        mockMvc.perform(put("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "100.00", pIdExtrano, "2026-09-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editar_gastoInexistente_devuelve404() throws Exception {
        mockMvc.perform(put("/api/grupos/{id}/gastos/{gid}", grupoId, 999_999_999L)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "100.00", pIdA, "2026-09-01")))
                .andExpect(status().isNotFound());
    }

    @Test
    void editar_usuarioNoMiembro_devuelve403() throws Exception {
        Long gastoId = objectMapper.readTree(
                registrarGasto(tokenA, grupoId, "100.00", pIdA, "2026-09-01")).get("id").asLong();

        mockMvc.perform(put("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenExtrano))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "100.00", pIdA, "2026-09-01")))
                .andExpect(status().isForbidden());
    }

    // 7.5 Eliminación -------------------------------------------

    @Test
    void eliminar_porMiembroNoPagador_devuelve204YLuego404() throws Exception {
        Long gastoId = objectMapper.readTree(
                registrarGasto(tokenA, grupoId, "100.00", pIdA, "2026-09-01")).get("id").asLong();

        String cuerpo = mockMvc.perform(delete("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).isEmpty();

        mockMvc.perform(get("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_gastoInexistente_devuelve404() throws Exception {
        mockMvc.perform(delete("/api/grupos/{id}/gastos/{gid}", grupoId, 999_999_999L)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_usuarioNoMiembro_devuelve403() throws Exception {
        Long gastoId = objectMapper.readTree(
                registrarGasto(tokenA, grupoId, "100.00", pIdA, "2026-09-01")).get("id").asLong();

        mockMvc.perform(delete("/api/grupos/{id}/gastos/{gid}", grupoId, gastoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden());
    }

    // 7.6 Autenticación --------------------------------------

    @Test
    void endpointsGastos_sinToken_devuelven401() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/gastos", grupoId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "100.00", pIdA, "2026-09-01")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/grupos/{id}/gastos/{gid}", grupoId, 1L))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/grupos/{id}/gastos/{gid}", grupoId, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gastoBody("Cena", "100.00", pIdA, "2026-09-01")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/grupos/{id}/gastos/{gid}", grupoId, 1L))
                .andExpect(status().isUnauthorized());
    }
}
