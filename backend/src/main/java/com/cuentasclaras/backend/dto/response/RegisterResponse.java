package com.cuentasclaras.backend.dto.response;

public record RegisterResponse(
        String token,
        ParticipanteDto participante) {
}
