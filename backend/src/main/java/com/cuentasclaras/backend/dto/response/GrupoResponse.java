package com.cuentasclaras.backend.dto.response;

import java.util.List;

public record GrupoResponse(
        Long id,
        String nombre,
        String descripcion,
        ParticipanteDto creador,
        List<ParticipanteDto> miembros) {
}
