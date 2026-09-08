package com.cuentasclaras.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.BajaParticipante;
import com.cuentasclaras.backend.entity.BajaParticipanteId;

public interface BajaParticipanteRepository
        extends JpaRepository<BajaParticipante, BajaParticipanteId> {

    List<BajaParticipante> findByBajaId(Long bajaId);

    void deleteByBajaId(Long bajaId);
}
