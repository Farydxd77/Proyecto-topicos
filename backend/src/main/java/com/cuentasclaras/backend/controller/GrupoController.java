package com.cuentasclaras.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cuentasclaras.backend.dto.request.ActualizarGrupoRequest;
import com.cuentasclaras.backend.dto.request.AgregarMiembroRequest;
import com.cuentasclaras.backend.dto.request.CrearGrupoRequest;
import com.cuentasclaras.backend.dto.response.GrupoResponse;
import com.cuentasclaras.backend.dto.response.GrupoResumenDto;
import com.cuentasclaras.backend.service.GrupoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/grupos")
public class GrupoController {

    private final GrupoService grupoService;

    public GrupoController(GrupoService grupoService) {
        this.grupoService = grupoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoResponse crear(@Valid @RequestBody CrearGrupoRequest request) {
        return grupoService.crear(request);
    }

    @GetMapping
    public List<GrupoResumenDto> listarMisGrupos() {
        return grupoService.listarMisGrupos();
    }

    @GetMapping("/{id}")
    public GrupoResponse obtenerDetalle(@PathVariable Long id) {
        return grupoService.obtenerDetalle(id);
    }

    @PutMapping("/{id}")
    public GrupoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarGrupoRequest request) {
        return grupoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        grupoService.eliminar(id);
    }

    @PostMapping("/{id}/miembros")
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoResponse agregarMiembro(
            @PathVariable Long id,
            @Valid @RequestBody AgregarMiembroRequest request) {
        return grupoService.agregarMiembro(id, request);
    }

    @DeleteMapping("/{id}/miembros/{participanteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarMiembro(@PathVariable Long id, @PathVariable Long participanteId) {
        grupoService.quitarMiembro(id, participanteId);
    }
}
