package com.cuentasclaras.backend.dto.response;

public record GrupoResumenDto(
        Long id,
        String nombre,
        String descripcion,
        ParticipanteDto creador) {
}
