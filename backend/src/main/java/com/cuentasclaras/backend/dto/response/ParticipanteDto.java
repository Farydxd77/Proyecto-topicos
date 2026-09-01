package com.cuentasclaras.backend.dto.response;

public record ParticipanteDto(
        Long id,
        String nombre,
        String apellido,
        String ci,
        String username) {
}
