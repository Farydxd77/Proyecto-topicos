package com.cuentasclaras.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS gobernado por {@code cors.allowed-origins}.
 *
 * <p>En desarrollo la propiedad está vacía y esta clase no registra nada: el proxy
 * de Vite reenvía {@code /api} a {@code localhost:8080} del lado del servidor, así
 * que el navegador solo ve un origen y no hay petición cruzada.
 *
 * <p>Solo hace falta cuando el frontend y el backend se sirven desde dominios
 * distintos. En ese caso se listan los orígenes reales: nunca un comodín, porque
 * estas peticiones llevan la cabecera {@code Authorization} y Spring rechaza
 * combinar {@code *} con credenciales.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> origenesPermitidos;

    public CorsConfig(@Value("${cors.allowed-origins:}") List<String> origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos.stream()
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origenesPermitidos.isEmpty()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true);
    }
}
