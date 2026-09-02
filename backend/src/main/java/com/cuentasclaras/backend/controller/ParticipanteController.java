package com.cuentasclaras.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cuentasclaras.backend.dto.response.ParticipanteDto;
import com.cuentasclaras.backend.service.ParticipanteService;

@RestController
@RequestMapping("/api/participantes")
public class ParticipanteController {

    private final ParticipanteService participanteService;

    public ParticipanteController(ParticipanteService participanteService) {
        this.participanteService = participanteService;
    }

    @GetMapping
    public List<ParticipanteDto> listar(
            @RequestParam(required = false) String ci,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido) {
        return participanteService.listar(ci, nombre, apellido);
    }

    @GetMapping("/{id}")
    public ParticipanteDto obtenerPorId(@PathVariable Long id) {
        return participanteService.obtenerPorId(id);
    }
}
