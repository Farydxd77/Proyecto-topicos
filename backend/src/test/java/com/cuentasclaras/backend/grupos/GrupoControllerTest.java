package com.cuentasclaras.backend.grupos;

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
class GrupoControllerTest {

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
    private String extranoToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long marca = System.nanoTime();

        creadorUsername = "gr-creador-" + marca;
        creadorToken = registrar(creadorUsername, "Ana", "Perez", "CI-C" + marca);

        miembroUsername = "gr-miembro-" + marca;
        miembroToken = registrar(miembroUsername, "Beto", "Lopez", "CI-M" + marca);

        extranoToken = registrar("gr-extrano-" + marca, "Caro", "Diaz", "CI-E" + marca);
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

    private Long crearGrupo(String token, String nombre, String descripcion) throws Exception {
        String body = descripcion == null
                ? """
                        {"nombre":"%s"}
                        """.formatted(nombre)
                : """
                        {"nombre":"%s","descripcion":"%s"}
                        """.formatted(nombre, descripcion);
        String response = mockMvc.perform(post("/api/grupos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void agregarMiembro(String tokenCreador, Long grupoId, Long participanteId)
            throws Exception {
        mockMvc.perform(post("/api/grupos/{id}/miembros", grupoId)
                        .header("Authorization", bearer(tokenCreador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participanteId\":%d}".formatted(participanteId)))
                .andExpect(status().isCreated());
    }

    // 8.1 Creación ----------------------------------------------------------

    @Test
    void crearGrupo_datosValidos_devuelve201ConCreadorYUnicoMiembro() throws Exception {
        Long creadorId = participanteIdDe(creadorUsername);

        String response = mockMvc.perform(post("/api/grupos")
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Viaje a Uyuni","descripcion":"Enero 2026"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Viaje a Uyuni"))
                .andExpect(jsonPath("$.descripcion").value("Enero 2026"))
                .andExpect(jsonPath("$.creador.id").value(creadorId))
                .andExpect(jsonPath("$.creador.username").value(creadorUsername))
                .andExpect(jsonPath("$.miembros.length()").value(1))
                .andExpect(jsonPath("$.miembros[0].id").value(creadorId))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password").doesNotContain("secret123");
    }

    @Test
    void crearGrupo_sinDescripcion_devuelve201ConDescripcionNula() throws Exception {
        mockMvc.perform(post("/api/grupos")
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cumpleanios"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").doesNotExist())
                .andExpect(jsonPath("$.miembros.length()").value(1))
                .andExpect(jsonPath("$.miembros[0].username").value(creadorUsername));
    }

    @Test
    void crearGrupo_nombreVacio_devuelve400ConFormatoEstandar() throws Exception {
        mockMvc.perform(post("/api/grupos")
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","descripcion":"x"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/grupos"));

        mockMvc.perform(get("/api/grupos").header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void crearGrupo_nombreSoloEspacios_devuelve400() throws Exception {
        mockMvc.perform(post("/api/grupos")
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // 8.2 Listado -----------------------------------------------------------

    @Test
    void listarGrupos_miembroDeDosGrupos_devuelveAmbos() throws Exception {
        crearGrupo(creadorToken, "Grupo A", "primero");
        crearGrupo(creadorToken, "Grupo B", null);

        mockMvc.perform(get("/api/grupos").header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].creador.username").value(creadorUsername))
                .andExpect(jsonPath("$[?(@.nombre == 'Grupo A')].descripcion").value("primero"))
                .andExpect(jsonPath("$[?(@.nombre == 'Grupo B')]").isNotEmpty());
    }

    @Test
    void listarGrupos_usuarioSinGrupos_devuelve200ConArrayVacio() throws Exception {
        mockMvc.perform(get("/api/grupos").header("Authorization", bearer(extranoToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listarGrupos_noIncluyeGruposAjenos() throws Exception {
        crearGrupo(creadorToken, "Grupo ajeno", null);

        mockMvc.perform(get("/api/grupos").header("Authorization", bearer(miembroToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // 8.3 Detalle -----------------------------------------------------------

    @Test
    void detalleGrupo_miembro_devuelve200ConMiembrosSinPassword() throws Exception {
        Long grupoId = crearGrupo(creadorToken, "Grupo detalle", "desc");
        agregarMiembro(creadorToken, grupoId, participanteIdDe(miembroUsername));

        String response = mockMvc.perform(
                        get("/api/grupos/{id}", grupoId).header("Authorization", bearer(miembroToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Grupo detalle"))
                .andExpect(jsonPath("$.descripcion").value("desc"))
                .andExpect(jsonPath("$.creador.username").value(creadorUsername))
                .andExpect(jsonPath("$.miembros.length()").value(2))
                .andExpect(jsonPath("$.miembros[?(@.username == '" + creadorUsername + "')]").isNotEmpty())
                .andExpect(jsonPath("$.miembros[?(@.username == '" + miembroUsername + "')]").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("password").doesNotContain("secret123");
    }

    @Test
    void detalleGrupo_noMiembro_devuelve403ConFormatoEstandar() throws Exception {
        Long grupoId = crearGrupo(creadorToken, "Grupo privado", null);

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(extranoToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/grupos/" + grupoId));
    }

    @Test
    void detalleGrupo_inexistente_devuelve404ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/grupos/{id}", 999_999_999L)
                        .header("Authorization", bearer(creadorToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/grupos/999999999"));
    }

    // 8.4 Edición -----------------------------------------------------------

    @Test
    void editarGrupo_creador_devuelve200ConValoresActualizados() throws Exception {
        Long grupoId = crearGrupo(creadorToken, "Nombre viejo", "desc vieja");
        agregarMiembro(creadorToken, grupoId, participanteIdDe(miembroUsername));

        mockMvc.perform(put("/api/grupos/{id}", grupoId)
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Nombre nuevo","descripcion":"desc nueva"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(grupoId))
                .andExpect(jsonPath("$.nombre").value("Nombre nuevo"))
                .andExpect(jsonPath("$.descripcion").value("desc nueva"))
                .andExpect(jsonPath("$.creador.username").value(creadorUsername))
                .andExpect(jsonPath("$.miembros.length()").value(2));
    }

    @Test
    void editarGrupo_miembroNoCreador_devuelve403YNoModifica() throws Exception {
        Long grupoId = crearGrupo(creadorToken, "Intacto", null);
        agregarMiembro(creadorToken, grupoId, participanteIdDe(miembroUsername));

        mockMvc.perform(put("/api/grupos/{id}", grupoId)
                        .header("Authorization", bearer(miembroToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Hackeado"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Intacto"));
    }

    @Test
    void editarGrupo_noMiembro_devuelve403() throws Exception {
        Long grupoId = crearGrupo(creadorToken, "Ajeno", null);

        mockMvc.perform(put("/api/grupos/{id}", grupoId)
                        .header("Authorization", bearer(extranoToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Hackeado"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void editarGrupo_nombreVacio_devuelve400YNoModifica() throws Exception {
        Long grupoId = crearGrupo(creadorToken, "Sigue igual", null);

        mockMvc.perform(put("/api/grupos/{id}", grupoId)
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/grupos/" + grupoId));

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sigue igual"));
    }

    @Test
    void editarGrupo_inexistente_devuelve404() throws Exception {
        mockMvc.perform(put("/api/grupos/{id}", 999_999_999L)
                        .header("Authorization", bearer(creadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cualquiera"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 8.5 Eliminación -------------------------------------------------------

    @Test
    void eliminarGrupo_creador_devuelve204YLuegoNoExiste() throws Exception {
        Long grupoId = crearGrupo(creadorToken, "Para borrar", null);
        agregarMiembro(creadorToken, grupoId, participanteIdDe(miembroUsername));

        mockMvc.perform(delete("/api/grupos/{id}", grupoId)
                        .header("Authorization", bearer(creadorToken)))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(creadorToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/grupos").header("Authorization", bearer(miembroToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void eliminarGrupo_miembroNoCreador_devuelve403YGrupoSigueExistiendo() throws Exception {
        Long grupoId = crearGrupo(creadorToken, "No se borra", null);
        agregarMiembro(creadorToken, grupoId, participanteIdDe(miembroUsername));

        mockMvc.perform(delete("/api/grupos/{id}", grupoId)
                        .header("Authorization", bearer(miembroToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/grupos/{id}", grupoId).header("Authorization", bearer(creadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("No se borra"));
    }

    @Test
    void eliminarGrupo_inexistente_devuelve404() throws Exception {
        mockMvc.perform(delete("/api/grupos/{id}", 999_999_999L)
                        .header("Authorization", bearer(creadorToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 8.6 Autenticación -----------------------------------------------------

    @Test
    void endpointsGrupos_sinToken_devuelven401ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/grupos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/grupos"));

        mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sin token"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/grupos/{id}", 1L))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/grupos/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sin token"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/grupos/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }
}
