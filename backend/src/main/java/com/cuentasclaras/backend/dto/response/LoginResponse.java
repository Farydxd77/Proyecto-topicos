package com.cuentasclaras.backend.dto.response;

public record LoginResponse(
        String token,
        UsuarioDto usuario) {
}
