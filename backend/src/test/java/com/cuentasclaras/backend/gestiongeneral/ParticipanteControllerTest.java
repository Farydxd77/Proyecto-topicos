package com.cuentasclaras.backend.gestiongeneral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ParticipanteControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ParticipanteRepository participanteRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String username;
    private String token;
    private String nombre;
    private String apellido;
    private String ci;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long marca = System.nanoTime();
        username = "gg-part-" + marca;
        nombre = "Zora" + marca;
        apellido = "Quispe" + marca;
        ci = "CI" + marca;
        token = registrar(username, "secret123", nombre, apellido, ci);
    }

    private String registerBody(String user, String password, String nom, String ape, String cedula) {
        return """
                {"username":"%s","password":"%s","nombre":"%s","apellido":"%s","ci":"%s"}
                """.formatted(user, password, nom, ape, cedula);
    }

    private String registrar(String user, String password, String nom, String ape, String cedula)
            throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(user, password, nom, ape, cedula)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asString();
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private Long participanteIdDe(String user) {
        Long usuarioId = usuarioRepository.findByUsername(user).orElseThrow().getId();
        return participanteRepository.findByUsuarioId(usuarioId).orElseThrow().getId();
    }

    // 5.4 -------------------------------------------------------------------

    @Test
    void getParticipantes_conTokenValido_devuelveArraySinPassword() throws Exception {
        String response = mockMvc.perform(get("/api/participantes").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.username == '" + username + "')]").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password").doesNotContain("secret123");
        assertThat(response).contains(ci);
    }

    @Test
    void getParticipantePorId_existente_devuelve200() throws Exception {
        Long id = participanteIdDe(username);
        String response = mockMvc.perform(
                        get("/api/participantes/{id}", id).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andExpect(jsonPath("$.apellido").value(apellido))
                .andExpect(jsonPath("$.ci").value(ci))
                .andExpect(jsonPath("$.username").value(username))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password");
    }

    @Test
    void getParticipantePorId_inexistente_devuelve404ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/participantes/{id}", 999_999_999L).header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/participantes/999999999"));
    }

    // 5.5 -------------------------------------------------------------------

    @Test
    void getParticipantes_filtroNombreParcial_ignoraMayusculas() throws Exception {
        mockMvc.perform(get("/api/participantes")
                        .param("nombre", nombre.toLowerCase())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(username));
    }

    @Test
    void getParticipantes_filtroApellidoParcial_ignoraMayusculas() throws Exception {
        mockMvc.perform(get("/api/participantes")
                        .param("apellido", apellido.toUpperCase())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(username));
    }

    @Test
    void getParticipantes_filtroCiExacto_devuelveArrayConUnElemento() throws Exception {
        mockMvc.perform(get("/api/participantes")
                        .param("ci", ci)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ci").value(ci))
                .andExpect(jsonPath("$[0].username").value(username));
    }

    @Test
    void getParticipantes_busquedaSinResultados_devuelve200ConArrayVacio() throws Exception {
        mockMvc.perform(get("/api/participantes")
                        .param("ci", "no-existe-" + System.nanoTime())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getParticipantes_ciYNombreJuntos_aplicaSoloCi() throws Exception {
        mockMvc.perform(get("/api/participantes")
                        .param("ci", ci)
                        .param("nombre", "otro-nombre-que-no-coincide")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ci").value(ci))
                .andExpect(jsonPath("$[0].username").value(username));
    }

    // 5.6 -------------------------------------------------------------------

    @Test
    void endpointsParticipantes_sinToken_devuelven401ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/participantes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/participantes"));

        mockMvc.perform(get("/api/participantes/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }
}
