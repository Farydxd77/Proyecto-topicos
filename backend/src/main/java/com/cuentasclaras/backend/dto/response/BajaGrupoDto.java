package com.cuentasclaras.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * La salida de un participante con saldo pendiente y la decisión del grupo.
 *
 * @param saldo   el balance congelado al momento de salir: negativo si debía
 * @param estado  {@code PENDIENTE}, {@code ASUMIDA} o {@code NO_ASUMIDA}
 * @param reparto quién absorbió cuánto; vacío salvo que el estado sea {@code ASUMIDA}
 */
public record BajaGrupoDto(
        Long id,
        Long grupoId,
        ParticipanteDto participante,
        BigDecimal saldo,
        String estado,
        LocalDate fecha,
        List<BajaParticipanteDto> reparto) {
}
