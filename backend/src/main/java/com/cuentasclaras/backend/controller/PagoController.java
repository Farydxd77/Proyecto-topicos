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

import com.cuentasclaras.backend.dto.request.ActualizarPagoRequest;
import com.cuentasclaras.backend.dto.request.RegistrarPagoRequest;
import com.cuentasclaras.backend.dto.response.PagoResponse;
import com.cuentasclaras.backend.service.PagoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/grupos/{grupoId}/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponse registrar(
            @PathVariable Long grupoId,
            @Valid @RequestBody RegistrarPagoRequest request) {
        return pagoService.registrar(grupoId, request);
    }

    @GetMapping
    public List<PagoResponse> listar(@PathVariable Long grupoId) {
        return pagoService.listar(grupoId);
    }

    @GetMapping("/{pagoId}")
    public PagoResponse obtenerDetalle(
            @PathVariable Long grupoId,
            @PathVariable Long pagoId) {
        return pagoService.obtenerDetalle(grupoId, pagoId);
    }

    @PutMapping("/{pagoId}")
    public PagoResponse actualizar(
            @PathVariable Long grupoId,
            @PathVariable Long pagoId,
            @Valid @RequestBody ActualizarPagoRequest request) {
        return pagoService.actualizar(grupoId, pagoId, request);
    }

    @DeleteMapping("/{pagoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable Long grupoId,
            @PathVariable Long pagoId) {
        pagoService.eliminar(grupoId, pagoId);
    }
}
