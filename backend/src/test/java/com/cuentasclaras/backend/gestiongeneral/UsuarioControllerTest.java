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

import com.cuentasclaras.backend.repository.UsuarioRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=integration-test-secret-1234567890-1234567890-abcdef",
        "spring.jpa.hibernate.ddl-auto=update"
})
class UsuarioControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String username;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        username = "gg-usr-" + System.nanoTime();
        token = registrar(username, "secret123", "Ana", "Pérez", "1234567");
    }

    private String registerBody(String user, String password, String nombre, String apellido, String ci) {
        return """
                {"username":"%s","password":"%s","nombre":"%s","apellido":"%s","ci":"%s"}
                """.formatted(user, password, nombre, apellido, ci);
    }

    private String registrar(String user, String password, String nombre, String apellido, String ci)
            throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(user, password, nombre, apellido, ci)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asString();
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private Long idDe(String user) {
        return usuarioRepository.findByUsername(user).orElseThrow().getId();
    }

    // 5.1 -------------------------------------------------------------------

    @Test
    void getUsuarios_conTokenValido_devuelveArraySinPassword() throws Exception {
        String response = mockMvc.perform(get("/api/usuarios").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.username == '" + username + "')].id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password").doesNotContain("secret123");
    }

    @Test
    void getUsuarios_busquedaSinResultados_devuelve200ConArrayVacio() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .param("username", "no-existe-" + System.nanoTime())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // 5.2 -------------------------------------------------------------------

    @Test
    void getUsuarios_filtroUsername_coincidenciaParcialIgnoraMayusculas() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .param("username", username.toUpperCase())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value(username))
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void getUsuarioPorId_existente_devuelve200SinPassword() throws Exception {
        Long id = idDe(username);
        String response = mockMvc.perform(get("/api/usuarios/{id}", id).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value(username))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password");
    }

    @Test
    void getUsuarioPorId_inexistente_devuelve404ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/usuarios/{id}", 999_999_999L).header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/usuarios/999999999"));
    }

    // 5.3 -------------------------------------------------------------------

    @Test
    void getParticipanteDeUsuario_conParticipante_devuelve200() throws Exception {
        Long id = idDe(username);
        String response = mockMvc.perform(
                        get("/api/usuarios/{id}/participante", id).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Pérez"))
                .andExpect(jsonPath("$.ci").value("1234567"))
                .andExpect(jsonPath("$.username").value(username))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password");
    }

    @Test
    void getParticipanteDeUsuario_usuarioInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/usuarios/{id}/participante", 999_999_999L)
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/usuarios/999999999/participante"));
    }

    // 5.6 -------------------------------------------------------------------

    @Test
    void endpointsUsuarios_sinToken_devuelven401ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/usuarios"));

        mockMvc.perform(get("/api/usuarios/{id}", 1L))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/usuarios/{id}/participante", 1L))
                .andExpect(status().isUnauthorized());
    }
}
