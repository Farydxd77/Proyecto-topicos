package com.cuentasclaras.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.GrupoParticipante;
import com.cuentasclaras.backend.entity.GrupoParticipanteId;

public interface GrupoParticipanteRepository
        extends JpaRepository<GrupoParticipante, GrupoParticipanteId> {

    Optional<GrupoParticipante> findByGrupoIdAndParticipanteId(Long grupoId, Long participanteId);

    List<GrupoParticipante> findByGrupoId(Long grupoId);
}
