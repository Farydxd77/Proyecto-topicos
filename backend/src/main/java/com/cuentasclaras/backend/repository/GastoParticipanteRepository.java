package com.cuentasclaras.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.GastoParticipante;
import com.cuentasclaras.backend.entity.GastoParticipanteId;

public interface GastoParticipanteRepository
        extends JpaRepository<GastoParticipante, GastoParticipanteId> {

    List<GastoParticipante> findByGastoId(Long gastoId);

    void deleteByGastoId(Long gastoId);
}
