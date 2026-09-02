package com.cuentasclaras.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cuentasclaras.backend.dto.request.ActualizarPerfilRequest;
import com.cuentasclaras.backend.dto.request.CambiarPasswordRequest;
import com.cuentasclaras.backend.dto.request.CambiarUsernameRequest;
import com.cuentasclaras.backend.dto.response.PerfilResponse;
import com.cuentasclaras.backend.service.PerfilService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/me")
    public PerfilResponse obtenerMiPerfil() {
        return perfilService.obtenerMiPerfil();
    }

    @PutMapping("/me")
    public PerfilResponse actualizarMiPerfil(@Valid @RequestBody ActualizarPerfilRequest request) {
        return perfilService.actualizarMiPerfil(request);
    }

    @PutMapping("/me/username")
    public PerfilResponse cambiarUsername(@Valid @RequestBody CambiarUsernameRequest request) {
        return perfilService.cambiarUsername(request);
    }

    @PutMapping("/me/password")
    public void cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request) {
        perfilService.cambiarPassword(request);
    }
}
