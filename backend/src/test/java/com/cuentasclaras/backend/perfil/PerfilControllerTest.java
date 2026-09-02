package com.cuentasclaras.backend.perfil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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

import com.cuentasclaras.backend.entity.Participante;
import com.cuentasclaras.backend.repository.ParticipanteRepository;
import com.cuentasclaras.backend.repository.UsuarioRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=integration-test-secret-1234567890-1234567890-abcdef",
        "spring.jpa.hibernate.ddl-auto=update"
})
class PerfilControllerTest {

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

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        username = "perfil-it-" + System.nanoTime();
        token = registrarYObtenerToken(username, "secret123", "Ana", "Pérez", "1234567");
    }

    private String registerBody(String user, String password, String nombre, String apellido, String ci) {
        return """
                {"username":"%s","password":"%s","nombre":"%s","apellido":"%s","ci":"%s"}
                """.formatted(user, password, nombre, apellido, ci);
    }

    private String registrarYObtenerToken(String user, String password, String nombre, String apellido, String ci)
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

    // 5.1 -------------------------------------------------------------------

    @Test
    void getMe_conTokenValido_devuelve200SinPassword() throws Exception {
        String response = mockMvc.perform(get("/api/perfil/me").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").isNumber())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Pérez"))
                .andExpect(jsonPath("$.ci").value("1234567"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password").doesNotContain("secret123");
    }

    // 5.2 -------------------------------------------------------------------

    @Test
    void putMe_datosValidos_actualizaNombreYApellido() throws Exception {
        String response = mockMvc.perform(put("/api/perfil/me")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Anabel","apellido":"Gómez"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Anabel"))
                .andExpect(jsonPath("$.apellido").value("Gómez"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password");

        Participante persistido = participanteRepository
                .findByUsuarioId(usuarioRepository.findByUsername(username).orElseThrow().getId())
                .orElseThrow();
        assertThat(persistido.getNombre()).isEqualTo("Anabel");
        assertThat(persistido.getApellido()).isEqualTo("Gómez");
    }

    @Test
    void putMe_ciEnCuerpo_seIgnora() throws Exception {
        mockMvc.perform(put("/api/perfil/me")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez","ci":"9999999"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ci").value("1234567"));

        Participante persistido = participanteRepository
                .findByUsuarioId(usuarioRepository.findByUsername(username).orElseThrow().getId())
                .orElseThrow();
        assertThat(persistido.getCi()).isEqualTo("1234567");
    }

    @Test
    void putMe_nombreVacio_devuelve400ConFormatoEstandar() throws Exception {
        mockMvc.perform(put("/api/perfil/me")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","apellido":"Pérez"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/perfil/me"))
                .andExpect(jsonPath("$.errors.nombre").isNotEmpty());
    }

    // 5.3 -------------------------------------------------------------------

    @Test
    void putUsername_disponible_devuelve200() throws Exception {
        String nuevo = "perfil-it-nuevo-" + System.nanoTime();
        mockMvc.perform(put("/api/perfil/me/username")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s"}
                                """.formatted(nuevo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(nuevo));

        assertThat(usuarioRepository.findByUsername(nuevo)).isPresent();
    }

    @Test
    void putUsername_yaEnUso_devuelve409() throws Exception {
        String otro = "perfil-it-otro-" + System.nanoTime();
        registrarYObtenerToken(otro, "secret123", "Beto", "López", "7654321");

        mockMvc.perform(put("/api/perfil/me/username")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s"}
                                """.formatted(otro)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.path").value("/api/perfil/me/username"));

        assertThat(usuarioRepository.findByUsername(username)).isPresent();
    }

    @Test
    void putUsername_muyCorto_devuelve400() throws Exception {
        mockMvc.perform(put("/api/perfil/me/username")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ab"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.username").isNotEmpty());
    }

    // 5.4 -------------------------------------------------------------------

    @Test
    void putPassword_valida_devuelve200YPermiteLogin() throws Exception {
        mockMvc.perform(put("/api/perfil/me/password")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"nuevaClave1"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"nuevaClave1"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        assertThat(usuarioRepository.findByUsername(username).orElseThrow().getPassword())
                .isNotEqualTo("nuevaClave1");
    }

    @Test
    void putPassword_muyCorta_devuelve400() throws Exception {
        mockMvc.perform(put("/api/perfil/me/password")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"corta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.password").isNotEmpty());
    }

    // 5.5 -------------------------------------------------------------------

    @Test
    void endpointsPerfil_sinToken_devuelven401() throws Exception {
        mockMvc.perform(get("/api/perfil/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/perfil/me"));

        mockMvc.perform(put("/api/perfil/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/perfil/me/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"cualquiera"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/perfil/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"nuevaClave1"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
