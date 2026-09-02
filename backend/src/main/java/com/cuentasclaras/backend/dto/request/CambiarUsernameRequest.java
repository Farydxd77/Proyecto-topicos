package com.cuentasclaras.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarUsernameRequest(
        @NotBlank @Size(min = 3, max = 50) String username) {
}
