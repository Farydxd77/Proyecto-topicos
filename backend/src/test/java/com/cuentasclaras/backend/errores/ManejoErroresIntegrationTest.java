package com.cuentasclaras.backend.errores;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import tools.jackson.databind.ObjectMapper;

/**
 * Verifica la capacidad `manejo-errores`: toda respuesta de error de la API usa el
 * mismo cuerpo JSON, incluidas las que origina la infraestructura web y no la
 * lógica de negocio.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=integration-test-secret-1234567890-1234567890-abcdef",
        "spring.jpa.hibernate.ddl-auto=update"
})
class ManejoErroresIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String token;
    private Long grupoId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long marca = System.nanoTime();

        String body = """
                {"username":"err-%d","password":"secret123","nombre":"Ana","apellido":"Perez","ci":"CI-E%d"}
                """.formatted(marca, marca);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(response).get("token").asString();

        String grupo = mockMvc.perform(post("/api/grupos")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Grupo errores %d\"}".formatted(marca)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        grupoId = objectMapper.readTree(grupo).get("id").asLong();
    }

    private String bearer() {
        return "Bearer " + token;
    }

    /** Los cinco campos que la capacidad exige en CUALQUIER respuesta de error. */
    private ResultActions esperarFormatoEstandar(ResultActions actions, int status, String path)
            throws Exception {
        return actions
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(path));
    }

    @Test
    void jsonMalformado_devuelve400Estandar() throws Exception {
        esperarFormatoEstandar(
                mockMvc.perform(post("/api/grupos")
                                .header("Authorization", bearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\": "))
                        .andExpect(status().isBadRequest()),
                400, "/api/grupos");
    }

    @Test
    void cuerpoVacio_devuelve400Estandar() throws Exception {
        esperarFormatoEstandar(
                mockMvc.perform(post("/api/grupos")
                                .header("Authorization", bearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                        .andExpect(status().isBadRequest()),
                400, "/api/grupos");
    }

    @Test
    void fechaInvalida_devuelve400Estandar() throws Exception {
        String path = "/api/grupos/" + grupoId + "/gastos";
        esperarFormatoEstandar(
                mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                                .header("Authorization", bearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"descripcion":"Cena","monto":100.00,"moneda":"USDT",
                                         "pagadorId":1,"fecha":"no-es-una-fecha"}
                                        """))
                        .andExpect(status().isBadRequest()),
                400, path);
    }

    @Test
    void montoNoNumerico_devuelve400Estandar() throws Exception {
        String path = "/api/grupos/" + grupoId + "/gastos";
        esperarFormatoEstandar(
                mockMvc.perform(post("/api/grupos/{id}/gastos", grupoId)
                                .header("Authorization", bearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"descripcion":"Cena","monto":"mucho","moneda":"USDT",
                                         "pagadorId":1,"fecha":"2026-09-08"}
                                        """))
                        .andExpect(status().isBadRequest()),
                400, path);
    }

    @Test
    void idNoNumerico_devuelve400EstandarConNombreDeParametro() throws Exception {
        esperarFormatoEstandar(
                mockMvc.perform(get("/api/grupos/{id}", "abc").header("Authorization", bearer()))
                        .andExpect(status().isBadRequest()),
                400, "/api/grupos/abc")
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("id")));
    }

    @Test
    void metodoNoPermitido_devuelve405Estandar() throws Exception {
        esperarFormatoEstandar(
                mockMvc.perform(patch("/api/grupos").header("Authorization", bearer()))
                        .andExpect(status().isMethodNotAllowed()),
                405, "/api/grupos");
    }

    @Test
    void rutaInexistente_devuelve404Estandar() throws Exception {
        esperarFormatoEstandar(
                mockMvc.perform(get("/api/no-existe-esta-ruta").header("Authorization", bearer()))
                        .andExpect(status().isNotFound()),
                404, "/api/no-existe-esta-ruta");
    }

    @Test
    void errorDeNegocio_devuelve404Estandar() throws Exception {
        esperarFormatoEstandar(
                mockMvc.perform(get("/api/grupos/{id}", 999_999_999L).header("Authorization", bearer()))
                        .andExpect(status().isNotFound()),
                404, "/api/grupos/999999999");
    }

    @Test
    void errorDeValidacion_mantieneElFormatoEstandarYAgregaErrors() throws Exception {
        esperarFormatoEstandar(
                mockMvc.perform(post("/api/grupos")
                                .header("Authorization", bearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"\"}"))
                        .andExpect(status().isBadRequest()),
                400, "/api/grupos")
                .andExpect(jsonPath("$.errors.nombre").exists());
    }

    @Test
    void sinToken_mantieneElFormatoEstandar() throws Exception {
        esperarFormatoEstandar(
                mockMvc.perform(get("/api/grupos")).andExpect(status().isUnauthorized()),
                401, "/api/grupos");
    }
}
