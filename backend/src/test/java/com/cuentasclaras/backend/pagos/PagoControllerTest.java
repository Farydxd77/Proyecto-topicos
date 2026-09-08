package com.cuentasclaras.backend.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=integration-test-secret-1234567890-1234567890-abcdef",
        "spring.jpa.hibernate.ddl-auto=update"
})
class PagoControllerTest {

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
    private String usuarioC;
    private String tokenExtrano;

    private Long grupoId;
    private Long pIdA;
    private Long pIdB;
    private Long pIdC;
    private Long pIdExtrano;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long marca = System.nanoTime();

        usuarioA = "pa-a-" + marca;
        tokenA = registrar(usuarioA, "Ana", "Perez", "CI-A" + marca);
        usuarioB = "pa-b-" + marca;
        tokenB = registrar(usuarioB, "Beto", "Lopez", "CI-B" + marca);
        usuarioC = "pa-c-" + marca;
        registrar(usuarioC, "Caro", "Diaz", "CI-C" + marca);
        tokenExtrano = registrar("pa-x-" + marca, "Equis", "Equis", "CI-X" + marca);

        pIdA = participanteIdDe(usuarioA);
        pIdB = participanteIdDe(usuarioB);
        pIdC = participanteIdDe(usuarioC);
        pIdExtrano = participanteIdDe("pa-x-" + marca);

        grupoId = crearGrupo(tokenA, "Viaje " + marca);
        agregarMiembro(tokenA, grupoId, pIdB);
        agregarMiembro(tokenA, grupoId, pIdC);
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

    private String pagoBody(Long receptorId, String monto, String fecha) {
        return """
                {"receptorId":%d,"monto":%s,"fecha":"%s"}
                """.formatted(receptorId, monto, fecha);
    }

    private String pagoBodyConTx(Long receptorId, String monto, String fecha, String txId) {
        return """
                {"receptorId":%d,"monto":%s,"fecha":"%s","txId":"%s"}
                """.formatted(receptorId, monto, fecha, txId);
    }

    private Long registrarPago(String token, Long receptorId, String monto, String fecha) throws Exception {
        String response = mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(receptorId, monto, fecha)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    // 1. Registro -----------------------------------------------------

    @Test
    void registrar_datosValidos_devuelve201ConPago() throws Exception {
        String response = mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "150.00", "2026-09-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.grupoId").value(grupoId))
                .andExpect(jsonPath("$.pagador.username").value(usuarioA))
                .andExpect(jsonPath("$.receptor.username").value(usuarioB))
                .andExpect(jsonPath("$.monto").value(150.00))
                .andExpect(jsonPath("$.fecha").value("2026-09-01"))
                .andExpect(jsonPath("$.txId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password").doesNotContain("secret123");
    }

    @Test
    void registrar_conTxId_loGuarda() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBodyConTx(pIdB, "10.00", "2026-09-01", "0xabc123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.txId").value("0xabc123"));
    }

    @Test
    void registrar_montoCero_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "0.00", "2026-09-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/grupos/" + grupoId + "/pagos"));
    }

    @Test
    void registrar_montoNegativo_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "-5.00", "2026-09-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrar_receptorIdAusente_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":10.00,\"fecha\":\"2026-09-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.receptorId").isNotEmpty());
    }

    @Test
    void registrar_fechaAusente_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorId\":%d,\"monto\":10.00}".formatted(pIdB)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fecha").isNotEmpty());
    }

    @Test
    void registrar_pagadorIgualReceptor_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdA, "10.00", "2026-09-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registrar_receptorNoMiembro_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdExtrano, "10.00", "2026-09-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registrar_usuarioNoMiembro_devuelve403() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenExtrano))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "10.00", "2026-09-01")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void registrar_grupoInexistente_devuelve404() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/pagos", 999_999_999L)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "10.00", "2026-09-01")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 2. Listado ----------------------------------------------------

    @Test
    void listar_grupoConPagos_devuelve200OrdenadoPorFechaDesc() throws Exception {
        registrarPago(tokenA, pIdB, "10.00", "2026-09-01");
        registrarPago(tokenB, pIdA, "20.00", "2026-09-05");

        mockMvc.perform(get("/api/grupos/{id}/pagos", grupoId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fecha").value("2026-09-05"))
                .andExpect(jsonPath("$[1].fecha").value("2026-09-01"))
                .andExpect(jsonPath("$[0].pagador.username").value(usuarioB));
    }

    @Test
    void listar_grupoSinPagos_devuelve200ConArrayVacio() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/pagos", grupoId).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listar_usuarioNoMiembro_devuelve403() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listar_grupoInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/pagos", 999_999_999L)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    // 3. Detalle --------------------------------------------------

    @Test
    void detalle_pagoDelGrupo_devuelve200() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "40.00", "2026-09-02");

        mockMvc.perform(get("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pagoId))
                .andExpect(jsonPath("$.pagador.username").value(usuarioA))
                .andExpect(jsonPath("$.receptor.username").value(usuarioB))
                .andExpect(jsonPath("$.monto").value(40.00));
    }

    @Test
    void detalle_pagoInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/pagos/{pid}", grupoId, 999_999_999L)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void detalle_pagoDeOtroGrupo_devuelve404() throws Exception {
        Long otroGrupo = crearGrupo(tokenA, "Otro");
        agregarMiembro(tokenA, otroGrupo, pIdB);
        String response = mockMvc.perform(post("/api/grupos/{id}/pagos", otroGrupo)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "15.00", "2026-09-02")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long pagoOtro = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/grupos/{id}/pagos/{pid}", grupoId, pagoOtro)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void detalle_usuarioNoMiembro_devuelve403() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "40.00", "2026-09-02");

        mockMvc.perform(get("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden());
    }

    // 4. Edición ------------------------------------------------

    @Test
    void editar_porPagador_cambiaMontoYReceptor_devuelve200() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "100.00", "2026-09-01");

        mockMvc.perform(put("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdC, "80.00", "2026-09-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receptor.username").value(usuarioC))
                .andExpect(jsonPath("$.monto").value(80.00))
                .andExpect(jsonPath("$.fecha").value("2026-09-03"))
                .andExpect(jsonPath("$.pagador.username").value(usuarioA));
    }

    @Test
    void editar_montoInvalido_devuelve400YPagoSinCambios() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "100.00", "2026-09-01");

        mockMvc.perform(put("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "0.00", "2026-09-03")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(100.00))
                .andExpect(jsonPath("$.fecha").value("2026-09-01"));
    }

    @Test
    void editar_receptorNoMiembro_devuelve400() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "100.00", "2026-09-01");

        mockMvc.perform(put("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdExtrano, "100.00", "2026-09-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editar_receptorIgualPagador_devuelve400() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "100.00", "2026-09-01");

        mockMvc.perform(put("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdA, "100.00", "2026-09-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editar_porMiembroNoPagador_devuelve403() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "100.00", "2026-09-01");

        mockMvc.perform(put("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdA, "50.00", "2026-09-02")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void editar_pagoInexistente_devuelve404() throws Exception {
        mockMvc.perform(put("/api/grupos/{id}/pagos/{pid}", grupoId, 999_999_999L)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "50.00", "2026-09-02")))
                .andExpect(status().isNotFound());
    }

    @Test
    void editar_usuarioNoMiembro_devuelve403() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "100.00", "2026-09-01");

        mockMvc.perform(put("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenExtrano))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "50.00", "2026-09-02")))
                .andExpect(status().isForbidden());
    }

    // 5. Eliminación ------------------------------------------

    @Test
    void eliminar_porPagador_devuelve204YLuego404() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "100.00", "2026-09-01");

        String cuerpo = mockMvc.perform(delete("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).isEmpty();

        mockMvc.perform(get("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_porMiembroNoPagador_devuelve403() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "100.00", "2026-09-01");

        mockMvc.perform(delete("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
    }

    @Test
    void eliminar_pagoInexistente_devuelve404() throws Exception {
        mockMvc.perform(delete("/api/grupos/{id}/pagos/{pid}", grupoId, 999_999_999L)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_usuarioNoMiembro_devuelve403() throws Exception {
        Long pagoId = registrarPago(tokenA, pIdB, "100.00", "2026-09-01");

        mockMvc.perform(delete("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden());
    }

    // 6. Autenticación --------------------------------------

    @Test
    void endpointsPagos_sinToken_devuelven401() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/pagos", grupoId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "10.00", "2026-09-01")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/grupos/{id}/pagos/{pid}", grupoId, 1L))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/grupos/{id}/pagos/{pid}", grupoId, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoBody(pIdB, "10.00", "2026-09-01")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/grupos/{id}/pagos/{pid}", grupoId, 1L))
                .andExpect(status().isUnauthorized());
    }
}
