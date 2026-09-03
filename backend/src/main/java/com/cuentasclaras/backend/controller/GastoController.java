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

import com.cuentasclaras.backend.dto.request.ActualizarGastoRequest;
import com.cuentasclaras.backend.dto.request.RegistrarGastoRequest;
import com.cuentasclaras.backend.dto.response.GastoResponse;
import com.cuentasclaras.backend.dto.response.GastoResumenDto;
import com.cuentasclaras.backend.service.GastoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/grupos/{grupoId}/gastos")
public class GastoController {

    private final GastoService gastoService;

    public GastoController(GastoService gastoService) {
        this.gastoService = gastoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GastoResponse registrar(
            @PathVariable Long grupoId,
            @Valid @RequestBody RegistrarGastoRequest request) {
        return gastoService.registrar(grupoId, request);
    }

    @GetMapping
    public List<GastoResumenDto> listar(@PathVariable Long grupoId) {
        return gastoService.listar(grupoId);
    }

    @GetMapping("/{gastoId}")
    public GastoResponse obtenerDetalle(
            @PathVariable Long grupoId,
            @PathVariable Long gastoId) {
        return gastoService.obtenerDetalle(grupoId, gastoId);
    }

    @PutMapping("/{gastoId}")
    public GastoResponse actualizar(
            @PathVariable Long grupoId,
            @PathVariable Long gastoId,
            @Valid @RequestBody ActualizarGastoRequest request) {
        return gastoService.actualizar(grupoId, gastoId, request);
    }

    @DeleteMapping("/{gastoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable Long grupoId,
            @PathVariable Long gastoId) {
        gastoService.eliminar(grupoId, gastoId);
    }
}
