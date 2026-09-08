package com.cuentasclaras.backend.infraestructura;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * CORS con orígenes configurados, que es el caso de producción cuando el frontend y
 * el backend se sirven desde dominios distintos.
 */
@SpringBootTest
@TestPropertySource(properties = "cors.allowed-origins=https://cuentasclaras.app")
class CorsConfigTest {

    private static final String ORIGEN = "https://cuentasclaras.app";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void preflightDesdeElOrigenPermitido_devuelveLasCabecerasDeCors() throws Exception {
        mockMvc.perform(options("/api/grupos")
                        .header("Origin", ORIGEN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN));
    }

    @Test
    void preflightDesdeOtroOrigen_esRechazado() throws Exception {
        mockMvc.perform(options("/api/grupos")
                        .header("Origin", "https://no-autorizado.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void losCuatroMetodosDeEscrituraYLecturaEstanPermitidos() throws Exception {
        for (String metodo : new String[] { "GET", "POST", "PUT", "DELETE" }) {
            mockMvc.perform(options("/api/grupos")
                            .header("Origin", ORIGEN)
                            .header("Access-Control-Request-Method", metodo))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN));
        }
    }

    @Test
    void nuncaSeDevuelveElComodin() throws Exception {
        // Estas peticiones llevan Authorization: el comodín sería un riesgo gratuito,
        // y además Spring no lo admite junto con credenciales.
        mockMvc.perform(options("/api/grupos")
                        .header("Origin", ORIGEN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().string("Access-Control-Allow-Origin",
                        org.hamcrest.Matchers.not("*")));
    }
}
