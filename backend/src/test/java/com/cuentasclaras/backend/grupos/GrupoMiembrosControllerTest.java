package com.cuentasclaras.backend.grupos;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class GrupoMiembrosControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ParticipanteRepository participanteRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private String creadorUsername;
    private String creadorToken;
    private String miembroUsername;
    private String miembroToken;
    private String invitadoUsername;

    private Long grupoId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long marca = System.nanoTime();

        creadorUsername = "gm-creador-" + marca;
        creadorToken = registrar(creadorUsername, "Ana", "Perez", "CI-C" + marca);

        miembroUsername = "gm-miembro-" + marca;
        miembroToken = registrar(miembroUsername, "Beto", "Lopez", "CI-M" + marca);

        invitadoUsername = "gm-invitado-" + marca;
        registrar(invitadoUsername, "Caro", "Diaz", "CI-I" + marca);

        grupoId = crearGrupo(creadorToken, "Grupo de miembros");
    }

    // Helpers ---------------------------------------------------------------

    private String registrar(String user, String nombre, String apellido, String ci)
            throws Exception {
        String body = """
                {"username":"%s","password":"secret123","nombre":"%s","apellido":"%s","ci":"%s"}
                """.formatted(user, nombre, apellido, ci);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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

    // 9.1 Alta de miembros --------------------------------------------------

    @Test
    void agregarMiembro_participanteNuevo_devuelve201YLoIncluye() throws Exception {
        Long miembroId = participanteIdDe(miembroUsername);

        mockMvc.perform(post("/api/grupos/{id}/miembros", grupoId)
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participanteId\":%d}".formatted(miembroId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(grupoId))
                .andExpect(jsonPath("$.miembros.length()").value(2))
                .andExpect(jsonPath("$.miembros[?(@.username == '" + miembroUsername + "')]").isNotEmpty())
                .andExpect(jsonPath("$.miembros[?(@.username == '" + creadorUsername + "')]").isNotEmpty());

        mockMvc.perform(get("/api/grupos").header("Authorization", bearer(miembroToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(grupoId));
    }

    @Test
    void agregarMiembro_yaEsMiembro_devuelve409YNoCambiaMembresia() throws Exception {
        Long miembroId = participanteIdDe(miembroUsername);
        agregarMiembro(creadorToken, grupoId, miembroId);

        mockMvc.perform(post("/api/grupos/{id}/miembros", grupoId)
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participanteId\":%d}".formatted(miembroId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/grupos/" + grupoId + "/miembros"));

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miembros.length()").value(2));
    }

    @Test
    void agregarMiembro_participanteInexistente_devuelve404() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/miembros", grupoId)
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participanteId\":999999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void agregarMiembro_sinParticipanteId_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/miembros", grupoId)
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void agregarMiembro_miembroNoCreador_devuelve403YNoCambiaMembresia() throws Exception {
        agregarMiembro(creadorToken, grupoId, participanteIdDe(miembroUsername));
        Long invitadoId = participanteIdDe(invitadoUsername);

        mockMvc.perform(post("/api/grupos/{id}/miembros", grupoId)
                        .header("Authorization", bearer(miembroToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participanteId\":%d}".formatted(invitadoId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miembros.length()").value(2));
    }

    // 9.2 Baja de miembros --------------------------------------------------

    @Test
    void quitarMiembro_creadorQuitaAOtro_devuelve204YDejaDeVerElGrupo() throws Exception {
        Long miembroId = participanteIdDe(miembroUsername);
        agregarMiembro(creadorToken, grupoId, miembroId);

        mockMvc.perform(delete("/api/grupos/{id}/miembros/{participanteId}", grupoId, miembroId)
                        .header("Authorization", bearer(creadorToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miembros.length()").value(1))
                .andExpect(jsonPath("$.miembros[0].username").value(creadorUsername));

        mockMvc.perform(get("/api/grupos").header("Authorization", bearer(miembroToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void quitarMiembro_participanteNoEsMiembro_devuelve404() throws Exception {
        Long invitadoId = participanteIdDe(invitadoUsername);

        mockMvc.perform(delete("/api/grupos/{id}/miembros/{participanteId}", grupoId, invitadoId)
                        .header("Authorization", bearer(creadorToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // 9.3 Autorización de la baja -------------------------------------------

    @Test
    void quitarMiembro_creadorASiMismo_devuelve400YMembresiaIntacta() throws Exception {
        Long creadorId = participanteIdDe(creadorUsername);
        agregarMiembro(creadorToken, grupoId, participanteIdDe(miembroUsername));

        mockMvc.perform(delete("/api/grupos/{id}/miembros/{participanteId}", grupoId, creadorId)
                        .header("Authorization", bearer(creadorToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path")
                        .value("/api/grupos/" + grupoId + "/miembros/" + creadorId));

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miembros.length()").value(2));
    }

    @Test
    void quitarMiembro_miembroNoCreadorQuitaAOtro_devuelve403() throws Exception {
        Long miembroId = participanteIdDe(miembroUsername);
        Long invitadoId = participanteIdDe(invitadoUsername);
        agregarMiembro(creadorToken, grupoId, miembroId);
        agregarMiembro(creadorToken, grupoId, invitadoId);

        mockMvc.perform(delete("/api/grupos/{id}/miembros/{participanteId}", grupoId, invitadoId)
                        .header("Authorization", bearer(miembroToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miembros.length()").value(3));
    }

    @Test
    void quitarMiembro_miembroIntentaAbandonarElGrupo_devuelve403YSigueSiendoMiembro()
            throws Exception {
        Long miembroId = participanteIdDe(miembroUsername);
        agregarMiembro(creadorToken, grupoId, miembroId);

        mockMvc.perform(delete("/api/grupos/{id}/miembros/{participanteId}", grupoId, miembroId)
                        .header("Authorization", bearer(miembroToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/grupos").header("Authorization", bearer(miembroToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(grupoId));
    }

    // Autenticación ---------------------------------------------------------

    @Test
    void endpointsMiembros_sinToken_devuelven401ConFormatoEstandar() throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/miembros", grupoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participanteId\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(delete("/api/grupos/{id}/miembros/{participanteId}", grupoId, 1L))
                .andExpect(status().isUnauthorized());
    }
}
