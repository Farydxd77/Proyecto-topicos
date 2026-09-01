package com.cuentasclaras.backend.security;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.cuentasclaras.backend.entity.Usuario;
import com.cuentasclaras.backend.repository.UsuarioRepository;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "jwt.secret=integration-test-secret-1234567890-1234567890-abcdef",
        "spring.jpa.hibernate.ddl-auto=update"
})
class SeguridadJwtIntegrationTest {

    private static final String PROTECTED_PATH = "/api/grupos";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private String username;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        username = "it-user-" + System.nanoTime();
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword("$2a$10$abcdefghijklmnopqrstuv");
        usuarioRepository.save(usuario);
    }

    @Test
    void healthCheck_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRoute_withoutToken_returns401WithStandardBody() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value(PROTECTED_PATH));
    }

    @Test
    void protectedRoute_withValidToken_isNotUnauthorized() throws Exception {
        String token = jwtUtil.generateToken(username);

        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().is(not(401)));
    }
}
