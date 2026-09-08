package com.cuentasclaras.backend.balances;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * Verifica la capacidad `resumen-grupo`.
 *
 * <p>El test central es {@link #resumen_grupoSaldado_pendienteCeroYPagadoMenorQueGastado()}:
 * fija por contrato que un grupo completamente saldado tiene `totalPagado` MENOR que
 * `totalGastado`. Es contraintuitivo y es correcto: quien paga un gasto cubre su
 * propia parte en ese momento y nunca se transfiere dinero a sí mismo.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=integration-test-secret-1234567890-1234567890-abcdef",
        "spring.jpa.hibernate.ddl-auto=update"
})
class ResumenGrupoControllerTest {

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
    private String tokenBeto;
    private String tokenCarla;
    private String tokenExtrano;
    private Long grupoId;
    private Long idAna;
    private Long idBeto;
    private Long idCarla;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long marca = System.nanoTime();

        tokenAna = registrar("res-ana-" + marca, "Ana", "Perez", "CI-A" + marca);
        tokenBeto = registrar("res-beto-" + marca, "Beto", "Lopez", "CI-B" + marca);
        tokenCarla = registrar("res-carla-" + marca, "Carla", "Diaz", "CI-C" + marca);
        tokenExtrano = registrar("res-x-" + marca, "Equis", "Equis", "CI-X" + marca);

        idAna = participanteIdDe("res-ana-" + marca);
        idBeto = participanteIdDe("res-beto-" + marca);
        idCarla = participanteIdDe("res-carla-" + marca);

        grupoId = crearGrupo(tokenAna, "Resumen " + marca);
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

    private void registrarGasto(String token, String monto, Long pagadorId) throws Exception {
        String body = """
                {"descripcion":"Cabana","monto":%s,"pagadorId":%d,"fecha":"2026-09-01"}
                """.formatted(monto, pagadorId);
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void registrarGastoConDivision(String token, String monto, Long pagadorId,
            String divisionJson) throws Exception {
        String body = """
                {"descripcion":"Cena","monto":%s,"pagadorId":%d,"fecha":"2026-09-02",
                 "division":%s}
                """.formatted(monto, pagadorId, divisionJson);
        mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private Long registrarPago(String token, Long receptorId, String monto) throws Exception {
        String body = """
                {"receptorId":%d,"monto":%s,"fecha":"2026-09-03"}
                """.formatted(receptorId, monto);
        String response = mockMvc.perform(post("/api/grupos/{id}/pagos", grupoId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private JsonNode resumen(String token) throws Exception {
        String response = mockMvc.perform(get("/api/grupos/{id}/resumen", grupoId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private BigDecimal monto(JsonNode resumen, String campo) {
        return new BigDecimal(resumen.get(campo).asString());
    }

    // 1. Casos base -------------------------------------------------------

    @Test
    void resumen_grupoSinGastos_todoEnCero() throws Exception {
        JsonNode r = resumen(tokenAna);

        assertThat(monto(r, "totalGastado")).isEqualByComparingTo("0.00");
        assertThat(monto(r, "totalPagado")).isEqualByComparingTo("0.00");
        assertThat(monto(r, "pendientePorSaldar")).isEqualByComparingTo("0.00");
        assertThat(monto(r, "miParte")).isEqualByComparingTo("0.00");
        assertThat(r.get("cantidadGastos").asInt()).isZero();
        assertThat(r.get("cantidadPagos").asInt()).isZero();
    }

    @Test
    void resumen_escenarioSamaipata_totalGastadoYPendiente() throws Exception {
        registrarGasto(tokenAna, "900.00", idAna);

        JsonNode r = resumen(tokenAna);

        assertThat(monto(r, "totalGastado")).isEqualByComparingTo("900.00");
        assertThat(monto(r, "totalPagado")).isEqualByComparingTo("0.00");
        // A Ana le deben 600: su propia parte de 300 no es deuda de nadie.
        assertThat(monto(r, "pendientePorSaldar")).isEqualByComparingTo("600.00");
        assertThat(monto(r, "miParte")).isEqualByComparingTo("300.00");
        assertThat(r.get("cantidadGastos").asInt()).isEqualTo(1);
    }

    // 2. La aclaración contable ------------------------------------------

    @Test
    void resumen_grupoSaldado_pendienteCeroYPagadoMenorQueGastado() throws Exception {
        registrarGasto(tokenAna, "900.00", idAna);
        registrarPago(tokenBeto, idAna, "300.00");
        registrarPago(tokenCarla, idAna, "300.00");

        JsonNode r = resumen(tokenAna);

        assertThat(monto(r, "pendientePorSaldar")).isEqualByComparingTo("0.00");
        assertThat(monto(r, "totalPagado")).isEqualByComparingTo("600.00");
        assertThat(monto(r, "totalGastado")).isEqualByComparingTo("900.00");
        // El grupo está saldado y aun así pagado < gastado. No es un error.
        assertThat(monto(r, "totalPagado")).isLessThan(monto(r, "totalGastado"));
    }

    @Test
    void resumen_trasUnPago_bajaElPendienteYSubeElPagado() throws Exception {
        registrarGasto(tokenAna, "900.00", idAna);
        JsonNode antes = resumen(tokenAna);

        registrarPago(tokenBeto, idAna, "300.00");
        JsonNode despues = resumen(tokenAna);

        assertThat(monto(antes, "pendientePorSaldar")).isEqualByComparingTo("600.00");
        assertThat(monto(despues, "pendientePorSaldar")).isEqualByComparingTo("300.00");
        assertThat(monto(despues, "totalPagado")).isEqualByComparingTo("300.00");
        assertThat(despues.get("cantidadPagos").asInt()).isEqualTo(1);

        // La invariante que sí cierra: pagado + pendiente = deuda total del grupo.
        assertThat(monto(despues, "totalPagado").add(monto(despues, "pendientePorSaldar")))
                .isEqualByComparingTo(monto(antes, "pendientePorSaldar"));
    }

    @Test
    void resumen_pagadorExcluidoDelGasto_pagadoIgualaGastado() throws Exception {
        // Ana paga 300 de algo que no consume: los otros dos le deben el total.
        registrarGastoConDivision(tokenAna, "300.00", idAna,
                """
                        [{"participanteId":%d,"peso":1},{"participanteId":%d,"peso":1}]
                        """.formatted(idBeto, idCarla));
        registrarPago(tokenBeto, idAna, "150.00");
        registrarPago(tokenCarla, idAna, "150.00");

        JsonNode r = resumen(tokenAna);

        assertThat(monto(r, "pendientePorSaldar")).isEqualByComparingTo("0.00");
        // Único caso donde coinciden: quien pagó no participaba del gasto.
        assertThat(monto(r, "totalPagado")).isEqualByComparingTo("300.00");
        assertThat(monto(r, "totalGastado")).isEqualByComparingTo("300.00");
        assertThat(monto(r, "miParte")).isEqualByComparingTo("0.00");
    }

    // 3. miParte respeta la división --------------------------------------

    @Test
    void resumen_miParteRespetaLaDivision() throws Exception {
        registrarGasto(tokenAna, "900.00", idAna);
        // Carla queda fuera de este segundo gasto.
        registrarGastoConDivision(tokenAna, "300.00", idAna,
                """
                        [{"participanteId":%d,"peso":1},{"participanteId":%d,"peso":1}]
                        """.formatted(idAna, idBeto));

        assertThat(monto(resumen(tokenCarla), "miParte")).isEqualByComparingTo("300.00");
        assertThat(monto(resumen(tokenBeto), "miParte")).isEqualByComparingTo("450.00");
        assertThat(monto(resumen(tokenAna), "miParte")).isEqualByComparingTo("450.00");
        // No es totalGastado / cantidadDeMiembros (que daría 400 para cada uno).
        assertThat(monto(resumen(tokenAna), "totalGastado")).isEqualByComparingTo("1200.00");
    }

    @Test
    void resumen_alEliminarUnPago_vuelveElPendiente() throws Exception {
        registrarGasto(tokenAna, "900.00", idAna);
        Long pagoId = registrarPago(tokenBeto, idAna, "300.00");

        assertThat(monto(resumen(tokenAna), "pendientePorSaldar")).isEqualByComparingTo("300.00");

        mockMvc.perform(delete("/api/grupos/{id}/pagos/{pid}", grupoId, pagoId)
                        .header("Authorization", bearer(tokenBeto)))
                .andExpect(status().isNoContent());

        JsonNode r = resumen(tokenAna);
        assertThat(monto(r, "totalPagado")).isEqualByComparingTo("0.00");
        assertThat(monto(r, "pendientePorSaldar")).isEqualByComparingTo("600.00");
        assertThat(r.get("cantidadPagos").asInt()).isZero();
    }

    // 4. Acceso -----------------------------------------------------------

    @Test
    void resumen_usuarioNoMiembro_devuelve403() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/resumen", grupoId)
                        .header("Authorization", bearer(tokenExtrano)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void resumen_grupoInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/resumen", 999_999_999L)
                        .header("Authorization", bearer(tokenAna)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void resumen_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}/resumen", grupoId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
