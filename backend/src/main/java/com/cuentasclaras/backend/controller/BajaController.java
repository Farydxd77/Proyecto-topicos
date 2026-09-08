package com.cuentasclaras.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cuentasclaras.backend.dto.request.ResolverBajaRequest;
import com.cuentasclaras.backend.dto.response.BajaGrupoDto;
import com.cuentasclaras.backend.service.BajaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/grupos/{grupoId}/bajas")
public class BajaController {

    private final BajaService bajaService;

    public BajaController(BajaService bajaService) {
        this.bajaService = bajaService;
    }

    /** Cualquier miembro del grupo puede verlas. */
    @GetMapping
    public List<BajaGrupoDto> listar(@PathVariable Long grupoId) {
        return bajaService.listar(grupoId);
    }

    /** Reservado al creador: decide si el grupo asume la deuda. */
    @PutMapping("/{bajaId}")
    public BajaGrupoDto resolver(
            @PathVariable Long grupoId,
            @PathVariable Long bajaId,
            @Valid @RequestBody ResolverBajaRequest request) {
        return bajaService.resolver(grupoId, bajaId, request);
    }
}
