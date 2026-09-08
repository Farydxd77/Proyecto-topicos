package com.cuentasclaras.backend.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifica la capacidad `infraestructura` en su configuración por defecto.
 *
 * <p>Los dos primeros tests son la red de seguridad que hace seguro tocar el resto
 * del proyecto: si alguien cambia `src/test/resources/application.properties` o
 * agrega una anotación que reintroduzca PostgreSQL, la suite deja de correr sin
 * Docker y —peor— empieza a escribir en la base de desarrollo. Acá falla primero.
 */
@SpringBootTest
class ConfiguracionDeTestTest {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private WebApplicationContext context;

    @Value("${spring.datasource.url}")
    private String urlConfigurada;

    @Value("${cors.allowed-origins:}")
    private String origenesConfigurados;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void laSuiteCorreContraUnaBaseEnMemoria() throws SQLException {
        try (Connection conexion = dataSource.getConnection()) {
            assertThat(conexion.getMetaData().getURL())
                    .as("Los tests deben correr contra H2 en memoria: sin Docker y sin "
                            + "tocar la base de desarrollo")
                    .startsWith("jdbc:h2:mem:")
                    .doesNotContain("postgresql");
        }
    }

    @Test
    void laBaseEnMemoriaEstaEnModoPostgreSql() {
        // Sin el modo de compatibilidad, H2 y PostgreSQL divergen lo suficiente como
        // para que un test pase acá y falle contra la base real.
        assertThat(urlConfigurada).contains("MODE=PostgreSQL");
    }

    @Test
    void porDefectoNoHayOrigenesDeCorsConfigurados() {
        assertThat(origenesConfigurados).isBlank();
    }

    @Test
    void sinOrigenesConfigurados_elPreflightNoDevuelveCabecerasDeCors() throws Exception {
        // En desarrollo el proxy de Vite evita la petición cruzada, así que registrar
        // CORS sería relajar la seguridad sin ninguna necesidad.
        mockMvc.perform(options("/api/grupos")
                        .header("Origin", "https://cualquier-otro.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
