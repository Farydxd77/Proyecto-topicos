package com.cuentasclaras.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cuentasclaras.backend.dto.response.BalanceDto;
import com.cuentasclaras.backend.dto.response.TransferenciaDto;
import com.cuentasclaras.backend.service.BalanceService;

@RestController
@RequestMapping("/api/grupos/{grupoId}")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balances")
    public List<BalanceDto> balances(@PathVariable Long grupoId) {
        return balanceService.calcularBalances(grupoId);
    }

    @GetMapping("/liquidacion")
    public List<TransferenciaDto> liquidacion(@PathVariable Long grupoId) {
        return balanceService.calcularLiquidacion(grupoId);
    }
}
