package com.cuentasclaras.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.dto.response.UsuarioDto;
import com.cuentasclaras.backend.service.ParticipanteService;
import com.cuentasclaras.backend.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ParticipanteService participanteService;

    public UsuarioController(UsuarioService usuarioService, ParticipanteService participanteService) {
        this.usuarioService = usuarioService;
        this.participanteService = participanteService;
    }

    @GetMapping
    public List<UsuarioDto> listar(@RequestParam(required = false) String username) {
        return usuarioService.listar(username);
    }

    @GetMapping("/{id}")
    public UsuarioDto obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id);
    }

    @GetMapping("/{id}/participante")
    public ParticipanteDto obtenerParticipante(@PathVariable Long id) {
        return participanteService.obtenerPorUsuarioId(id);
    }
}
