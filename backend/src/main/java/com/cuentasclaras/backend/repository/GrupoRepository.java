package com.cuentasclaras.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cuentasclaras.backend.entity.Grupo;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    List<Grupo> findByMiembrosParticipanteId(Long participanteId);
}
