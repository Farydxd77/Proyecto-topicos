package com.cuentasclaras.backend.dto.response;

import java.time.LocalDateTime;

public record PerfilResponse(
        Long id,
        Long usuarioId,
        String username,
        String nombre,
        String apellido,
        String ci,
        LocalDateTime createdAt) {
}
