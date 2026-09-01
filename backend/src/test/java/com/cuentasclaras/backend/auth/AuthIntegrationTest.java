package com.cuentasclaras.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.cuentasclaras.backend.entity.Usuario;
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
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ParticipanteRepository participanteRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String username;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        username = "auth-it-" + System.nanoTime();
    }

    private String registerBody(String user, String password, String nombre, String apellido, String ci) {
        return """
                {"username":"%s","password":"%s","nombre":"%s","apellido":"%s","ci":"%s"}
                """.formatted(user, password, nombre, apellido, ci);
    }

    private String doRegister(String body) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void register_validData_creates201WithTokenAndParticipante() throws Exception {
        String body = registerBody(username, "secret123", "Ana", "Pérez", "1234567");

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.participante.id").isNumber())
                .andExpect(jsonPath("$.participante.nombre").value("Ana"))
                .andExpect(jsonPath("$.participante.apellido").value("Pérez"))
                .andExpect(jsonPath("$.participante.ci").value("1234567"))
                .andExpect(jsonPath("$.participante.username").value(username))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password").doesNotContain("secret123");

        Usuario stored = usuarioRepository.findByUsername(username).orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", stored.getPassword())).isTrue();

        boolean participanteLinked = participanteRepository.findAll().stream()
                .anyMatch(p -> p.getUsuario() != null
                        && username.equals(p.getUsuario().getUsername()));
        assertThat(participanteLinked).isTrue();
    }

    @Test
    void register_returnedToken_passesProtectedRoute() throws Exception {
        String response = doRegister(registerBody(username, "secret123", "Ana", "Pérez", "1234567"));
        String token = objectMapper.readTree(response).get("token").asString();

        mockMvc.perform(get("/api/grupos").header("Authorization", "Bearer " + token))
                .andExpect(status().is(not(401)));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        String body = registerBody(username, "secret123", "Ana", "Pérez", "1234567");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void register_shortPassword_returns400WithFieldError() throws Exception {
        String body = registerBody(username, "abc", "Ana", "Pérez", "1234567");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.password").isNotEmpty());
    }

    @Test
    void register_missingFields_returns400ListingFields() throws Exception {
        String body = """
                {"username":"%s","password":"secret123","nombre":"","apellido":"","ci":""}
                """.formatted(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").isNotEmpty())
                .andExpect(jsonPath("$.errors.apellido").isNotEmpty())
                .andExpect(jsonPath("$.errors.ci").isNotEmpty());
    }

    @Test
    void login_correctCredentials_returns200WithToken() throws Exception {
        doRegister(registerBody(username, "secret123", "Ana", "Pérez", "1234567"));

        String loginBody = """
                {"username":"%s","password":"secret123"}
                """.formatted(username);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.username").value(username))
                .andExpect(jsonPath("$.usuario.id").isNumber())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password").doesNotContain("secret123");
    }

    @Test
    void login_wrongPassword_and_unknownUser_return401WithSameMessage() throws Exception {
        doRegister(registerBody(username, "secret123", "Ana", "Pérez", "1234567"));

        String wrongPass = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"WRONGpass1"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String unknownUser = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"no-existe-nunca","password":"WRONGpass1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        JsonNode a = objectMapper.readTree(wrongPass);
        JsonNode b = objectMapper.readTree(unknownUser);
        assertThat(a.get("message").asString()).isEqualTo(b.get("message").asString());
        assertThat(a.get("status").asInt()).isEqualTo(401);
    }
}
